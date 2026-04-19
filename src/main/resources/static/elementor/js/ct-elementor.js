(function($) {
    /**
     * @param $scope The Widget wrapper element as a jQuery element
     * @param $ The jQuery alias
     */
    var WidgetCTIlineHandler = function($scope, $) {
        setTimeout(function() {
            $(".elementor-top-section").each(function() {
                var _el_particle = $(this).find(
                        ".elementor-container .el-move-parents"
                    ),
                    _el_particle_remove = $(this).find(
                        ".elementor-widget-wrap .el-move-parents"
                    ),
                    _row_particle = $(this).find("> .elementor-container");
                _row_particle.before(_el_particle.clone());
                _el_particle_remove.remove();
            });
        }, 200);
    };

    function consultio_split_text($scope) {
        setTimeout(function() {
            var st = $scope.find(".pxl-split-text");
            if (st.length == 0) return;
            gsap.registerPlugin(SplitText);
            st.each(function(index, el) {
                el.split = new SplitText(el, {
                    type: "lines,words,chars",
                    linesClass: "split-line",
                });
                gsap.set(el, {
                    perspective: 400
                });

                if ($(el).hasClass("split-in-fade")) {
                    gsap.set(el.split.chars, {
                        opacity: 0,
                        ease: "Back.easeOut",
                    });
                }
                if ($(el).hasClass("split-in-right")) {
                    gsap.set(el.split.chars, {
                        opacity: 0,
                        x: "50",
                        ease: "Back.easeOut",
                    });
                }
                if ($(el).hasClass("split-in-left")) {
                    gsap.set(el.split.chars, {
                        opacity: 0,
                        x: "-50",
                        ease: "circ.out",
                    });
                }
                if ($(el).hasClass("split-in-up")) {
                    gsap.set(el.split.chars, {
                        opacity: 0,
                        y: "80",
                        ease: "circ.out",
                    });
                }
                if ($(el).hasClass("split-in-down")) {
                    gsap.set(el.split.chars, {
                        opacity: 0,
                        y: "-80",
                        ease: "circ.out",
                    });
                }
                if ($(el).hasClass("split-in-rotate")) {
                    gsap.set(el.split.chars, {
                        opacity: 0,
                        rotateX: "50deg",
                        ease: "circ.out",
                    });
                }
                if ($(el).hasClass("split-in-scale")) {
                    gsap.set(el.split.chars, {
                        opacity: 0,
                        scale: "0.5",
                        ease: "circ.out",
                    });
                }
                el.anim = gsap.to(el.split.chars, {
                    scrollTrigger: {
                        trigger: el,
                        toggleActions: "restart pause resume reverse",
                        start: "top 90%",
                    },
                    x: "0",
                    y: "0",
                    rotateX: "0",
                    scale: 1,
                    opacity: 1,
                    duration: 0.8,
                    stagger: 0.02,
                });
            });
        }, 500);
    }

    function consultio_text_marquee($scope) {
        const text_marquee = $scope.find(".pxl-text--marquee");

        const boxes = gsap.utils.toArray(text_marquee);

        const loop = text_horizontalLoop(boxes, {
            paused: false,
            repeat: -1
        });

        function text_horizontalLoop(items, config) {
            items = gsap.utils.toArray(items);
            config = config || {};
            let tl = gsap.timeline({
                    repeat: config.repeat,
                    paused: config.paused,
                    defaults: {
                        ease: "none"
                    },
                    onReverseComplete: () =>
                        tl.totalTime(tl.rawTime() + tl.duration() * 100),
                }),
                length = items.length,
                startX = items[0].offsetLeft,
                times = [],
                widths = [],
                xPercents = [],
                curIndex = 0,
                pixelsPerSecond = (config.speed || 1) * 100,
                snap =
                config.snap === false ?
                (v) => v :
                gsap.utils.snap(config.snap || 1),
                totalWidth,
                curX,
                distanceToStart,
                distanceToLoop,
                item,
                i;
            gsap.set(items, {
                xPercent: (i, el) => {
                    let w = (widths[i] = parseFloat(
                        gsap.getProperty(el, "width", "px")
                    ));
                    xPercents[i] = snap(
                        (parseFloat(gsap.getProperty(el, "x", "px")) / w) *
                        100 +
                        gsap.getProperty(el, "xPercent")
                    );
                    return xPercents[i];
                },
            });
            gsap.set(items, {
                x: 0
            });
            totalWidth =
                items[length - 1].offsetLeft +
                (xPercents[length - 1] / 100) * widths[length - 1] -
                startX +
                items[length - 1].offsetWidth *
                gsap.getProperty(items[length - 1], "scaleX") +
                (parseFloat(config.paddingRight) || 0);
            for (i = 0; i < length; i++) {
                item = items[i];
                curX = (xPercents[i] / 100) * widths[i];
                distanceToStart = item.offsetLeft + curX - startX;
                distanceToLoop =
                    distanceToStart +
                    widths[i] * gsap.getProperty(item, "scaleX");
                tl.to(
                        item, {
                            xPercent: snap(
                                ((curX - distanceToLoop) / widths[i]) * 100
                            ),
                            duration: distanceToLoop / pixelsPerSecond,
                        },
                        0
                    )
                    .fromTo(
                        item, {
                            xPercent: snap(
                                ((curX - distanceToLoop + totalWidth) /
                                    widths[i]) *
                                100
                            ),
                        }, {
                            xPercent: xPercents[i],
                            duration:
                                (curX - distanceToLoop + totalWidth - curX) /
                                pixelsPerSecond,
                            immediateRender: false,
                        },
                        distanceToLoop / pixelsPerSecond
                    )
                    .add("label" + i, distanceToStart / pixelsPerSecond);
                times[i] = distanceToStart / pixelsPerSecond;
            }

            function toIndex(index, vars) {
                vars = vars || {};
                Math.abs(index - curIndex) > length / 2 &&
                    (index += index > curIndex ? -length : length);
                let newIndex = gsap.utils.wrap(0, length, index),
                    time = times[newIndex];
                if (time > tl.time() !== index > curIndex) {
                    vars.modifiers = {
                        time: gsap.utils.wrap(0, tl.duration()),
                    };
                    time += tl.duration() * (index > curIndex ? 1 : -1);
                }
                curIndex = newIndex;
                vars.overwrite = true;
                return tl.tweenTo(time, vars);
            }
            tl.next = (vars) => toIndex(curIndex + 1, vars);
            tl.previous = (vars) => toIndex(curIndex - 1, vars);
            tl.current = () => curIndex;
            tl.toIndex = (index, vars) => toIndex(index, vars);
            tl.times = times;
            tl.progress(1, true).progress(0, true);
            if (config.reversed) {
                tl.vars.onReverseComplete();
                tl.reverse();
            }
            return tl;
        }
    }

    // Make sure you run this code under Elementor.
    $(window).on("elementor/frontend/init", function() {
        elementorFrontend.hooks.addAction(
            "frontend/element_ready/ct_logo_animate.default",
            WidgetCTIlineHandler
        );
        elementorFrontend.hooks.addAction(
            "frontend/element_ready/ct_particle_animate.default",
            WidgetCTIlineHandler
        );
        elementorFrontend.hooks.addAction(
            "frontend/element_ready/ct_portfolio_external.default",
            WidgetCTIlineHandler
        );

        elementorFrontend.hooks.addAction(
            "frontend/element_ready/ct_heading.default",
            function($scope) {
                consultio_split_text($scope);
            }
        );
        elementorFrontend.hooks.addAction(
            "frontend/element_ready/ct_text_marquee.default",
            function($scope) {
                consultio_text_marquee($scope);
            }
        );
    });
})(jQuery);