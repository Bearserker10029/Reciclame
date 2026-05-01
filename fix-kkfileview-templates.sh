#!/bin/bash
# Extrae los templates FTL reales desde el JAR de kkFileView, parcha el
# título dinámico y el nombre de descarga, y los deja listos para montar.
# Uso: ./fix-kkfileview-templates.sh
# Requiere: contenedor reciclame-kkfileview corriendo.

set -e

CONTAINER="reciclame-kkfileview"
JAR="/opt/kkFileView-4.1.0/bin/kkFileView-4.1.0.jar"
LOCAL_DIR="./kkfileview-templates/web"

echo "=== 1. Extrayendo templates del JAR (BOOT-INF/classes/web/*.ftl) ==="
docker exec "$CONTAINER" python3 -c "
import zipfile, os
z = zipfile.ZipFile('$JAR')
os.makedirs('/tmp/kkweb', exist_ok=True)
for name in z.namelist():
    if name.startswith('BOOT-INF/classes/web/') and name.endswith('.ftl'):
        data = z.read(name)
        out = '/tmp/kkweb/' + name.split('/')[-1]
        open(out, 'wb').write(data)
        print('  extraido:', name.split('/')[-1])
"

echo ""
echo "=== 2. Copiando al host: $LOCAL_DIR ==="
mkdir -p "$LOCAL_DIR"
docker cp "$CONTAINER:/tmp/kkweb/." "$LOCAL_DIR/"
echo "  $(ls "$LOCAL_DIR"/*.ftl | wc -l | tr -d ' ') archivos .ftl copiados"

echo ""
echo "=== 3. Parcheando <title> en todos los templates ==="
for f in "$LOCAL_DIR"/*.ftl; do
  perl -i -0pe 's|<title>[^<]*</title>|<title>\${(fileName)!""}</title>|gs' "$f"
  echo "  $(basename "$f")"
done

echo ""
echo "=== 4. Parcheando templates con Python ==="
python3 << 'PYEOF'
import re

# ppt.ftl: reemplazar "name": "PPT预览" hardcodeado con variable dinámica
path = './kkfileview-templates/web/ppt.ftl'
data = open(path, encoding='utf-8').read()
patched = re.sub(r'"name":\s*"[^"]*预览[^"]*"', '"name": "${(fileName)!\\"PPT\\"}"', data, count=1)
if patched != data:
    open(path, 'w', encoding='utf-8').write(patched)
    print('  ppt.ftl: name dinamico OK')
else:
    print('  ppt.ftl: patron no encontrado (revisar manualmente)')

# pdf.ftl: agregar &filename= al visor PDF para que la descarga use el nombre correcto
path = './kkfileview-templates/web/pdf.ftl'
data = open(path, encoding='utf-8').read()
old = 'viewer.html?file=" + encodeURIComponent(url) +'
new = 'viewer.html?file=" + encodeURIComponent(url) + "&filename=" + encodeURIComponent("${(fileName)!\\"documento\\"}") +'
if old in data:
    open(path, 'w', encoding='utf-8').write(data.replace(old, new, 1))
    print('  pdf.ftl: filename en pdfjs OK')
else:
    print('  pdf.ftl: patron no encontrado (revisar manualmente)')
PYEOF

echo ""
echo "=== 5. Verificando resultado ==="
echo "--- pdf.ftl ---"
grep -n "<title>\|filename" "$LOCAL_DIR/pdf.ftl"
echo "--- ppt.ftl ---"
grep -n '"name"' "$LOCAL_DIR/ppt.ftl" | head -2

echo ""
echo "=== LISTO ==="
echo "En docker-compose.yml, descomenta en la seccion kkfileview:"
echo ""
echo "    environment:"
echo "      - SPRING_FREEMARKER_TEMPLATE_LOADER_PATH=file:/opt/kkFileView/templates/web/,classpath:/web/"
echo "    volumes:"
echo "      - ./kkfileview-templates/web:/opt/kkFileView/templates/web"
echo ""
echo "Luego: docker compose up -d --force-recreate kkfileview"