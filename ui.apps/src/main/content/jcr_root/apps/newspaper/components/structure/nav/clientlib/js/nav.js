/**
 * Guardian-style Navigation  ·  newspaper/components/structure/nav
 *
 * Responsibilities:
 *  1. Mobile drawer: open/close via hamburger, close via backdrop/ESC
 *  2. Mobile L2 panel: open via expand button, close via back button/ESC
 *  3. Desktop: keyboard-driven subnav (CSS handles hover)
 *  4. Resize: close mobile drawer when switching to desktop
 *  5. Analytics: fire custom events
 */
(function () {
    'use strict';

    /* ------------------------------------------------------------------ */
    /*  Constants                                                            */
    /* ------------------------------------------------------------------ */
    var SEL_WRAPPER  = '[data-component="nav"]';
    var SEL_BURGER   = '.gdn-nav__burger';
    var SEL_DRAWER   = '.gdn-nav__drawer';
    var SEL_CLOSE    = '.gdn-nav__drawer-close';
    var SEL_EXPAND   = '.gdn-nav__drawer-expand';
    var SEL_BACKDROP = '.gdn-nav__backdrop';
    var SEL_MEGA     = '.gdn-nav__mega';

    var CLS_OPEN     = 'is-open';
    var CLS_MEGA     = 'is-mega-open';
    var CLS_VISIBLE  = 'is-visible';
    var DESKTOP_BP   = 1024;

    /* ------------------------------------------------------------------ */
    /*  Bootstrap                                                            */
    /* ------------------------------------------------------------------ */
    function init() {
        document.querySelectorAll(SEL_WRAPPER).forEach(initWrapper);
    }

    function initWrapper(wrapper) {
        if (wrapper.getAttribute('data-nav-initialized') === 'true') {
            return;
        }
        wrapper.setAttribute('data-nav-initialized', 'true');

        var burger   = wrapper.querySelector(SEL_BURGER);
        var drawer   = wrapper.querySelector(SEL_DRAWER);
        var closeBtn = wrapper.querySelector(SEL_CLOSE);
        var backdrop = wrapper.querySelector(SEL_BACKDROP);

        if (!burger) return;

        var lastIsDesktop = window.innerWidth >= DESKTOP_BP;

        /* ------------------------------------------------------------------
         * Event delegation (robust against HTL re-render / SPA)
         * ------------------------------------------------------------------ */
        wrapper.addEventListener('click', function (e) {
            var t = e.target;
            if (!t) return;

            var burgerBtn = t.closest && t.closest(SEL_BURGER);
            if (burgerBtn) {
                e.preventDefault();
                if (window.innerWidth >= DESKTOP_BP) {
                    toggleMega(wrapper, burger, backdrop);
                } else {
                    if (wrapper.classList.contains(CLS_OPEN)) {
                        closeDrawer(wrapper, burger, backdrop);
                    } else {
                        openDrawer(wrapper, burger, backdrop, drawer);
                    }
                }
                return;
            }

            var close = t.closest && t.closest(SEL_CLOSE);
            if (close) {
                e.preventDefault();
                if (window.innerWidth >= DESKTOP_BP) {
                    closeMega(wrapper, burger, backdrop);
                } else {
                    closeDrawer(wrapper, burger, backdrop);
                }
                return;
            }

            var backDrop = t.closest && t.closest(SEL_BACKDROP);
            if (backDrop) {
                e.preventDefault();
                if (window.innerWidth >= DESKTOP_BP) {
                    closeMega(wrapper, burger, backdrop);
                } else {
                    closeDrawer(wrapper, burger, backdrop);
                }
                return;
            }

            var expand = t.closest && t.closest(SEL_EXPAND);
            if (expand) {
                e.preventDefault();
                // Mobile accordion expand/collapse
                if (window.innerWidth < DESKTOP_BP) {
                    toggleAccordion(expand);
                }
                return;
            }
        });

        /* ---- ESC key ---- */
        document.addEventListener('keydown', function (e) {
            if (e.key !== 'Escape') return;

            /* Close open L2 first */
            // Mobile accordion: collapse any expanded sections first
            var expanded = wrapper.querySelector('.gdn-nav__drawer-item.is-expanded');
            if (expanded && window.innerWidth < DESKTOP_BP) {
                collapseAllAccordions(wrapper);
                return;
            }

            /* Close mega on desktop */
            if (wrapper.classList.contains(CLS_MEGA)) {
                closeMega(wrapper, burger, backdrop);
                return;
            }

            /* Then close the drawer */
            if (wrapper.classList.contains(CLS_OPEN)) {
                closeDrawer(wrapper, burger, backdrop);
            }
        });

        /* ---- Resize: close when switching to desktop ---- */
        var resizeTimer;
        window.addEventListener('resize', function () {
            clearTimeout(resizeTimer);
            resizeTimer = setTimeout(function () {
                var isDesktop = window.innerWidth >= DESKTOP_BP;

                // If breakpoint mode changed, hard reset to avoid stuck states
                if (isDesktop !== lastIsDesktop) {
                    closeDrawer(wrapper, burger, backdrop, true /* silent */);
                    closeMega(wrapper, burger, backdrop, true /* silent */);
                    collapseAllAccordions(wrapper);
                    lastIsDesktop = isDesktop;
                    return;
                }

                // Even without a mode change, ensure drawer doesn't remain open on desktop
                if (isDesktop) {
                    closeDrawer(wrapper, burger, backdrop, true /* silent */);
                    // mega may remain intentionally on desktop; keep as-is
                } else {
                    // On mobile/tablet ensure mega never remains open
                    closeMega(wrapper, burger, backdrop, true /* silent */);
                }
            }, 200);
        });

        /* ---- Desktop: keyboard access to subnav ---- */
        initDesktopKeyboard(wrapper);
    }

    /* ------------------------------------------------------------------ */
    /*  Drawer open / close                                                  */
    /* ------------------------------------------------------------------ */
    function openDrawer(wrapper, burger, backdrop, drawer) {
        wrapper.classList.add(CLS_OPEN);
        burger.setAttribute('aria-expanded', 'true');
        burger.setAttribute('aria-label', 'Close menu');
        document.body.style.overflow = 'hidden';

        if (drawer) {
            drawer.setAttribute('aria-hidden', 'false');
        }

        if (backdrop) {
            backdrop.style.display = 'block';
            /* Force reflow for CSS transition */
            void backdrop.offsetWidth;
            backdrop.classList.add(CLS_VISIBLE);
        }

        /* Focus first focusable element inside drawer */
        if (drawer) {
            var first = drawer.querySelector(
                'button:not([disabled]), a[href], [tabindex]:not([tabindex="-1"])'
            );
            if (first) setTimeout(function () { first.focus(); }, 50);
        }

        track('nav:open', { action: 'open' });
    }

    function closeDrawer(wrapper, burger, backdrop, silent) {
        // Collapse any mobile accordions
        collapseAllAccordions(wrapper);

        wrapper.classList.remove(CLS_OPEN);
        burger.setAttribute('aria-expanded', 'false');
        burger.setAttribute('aria-label', 'Open menu');
        document.body.style.overflow = '';

        var drawer = wrapper.querySelector(SEL_DRAWER);
        if (drawer) {
            drawer.setAttribute('aria-hidden', 'true');
        }

        if (backdrop) {
            backdrop.classList.remove(CLS_VISIBLE);
            setTimeout(function () { backdrop.style.display = ''; }, 280);
        }

        if (!silent) {
            burger.focus();
            track('nav:close', { action: 'close' });
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Desktop mega menu toggle                                            */
    /* ------------------------------------------------------------------ */
    function toggleMega(wrapper, burger, backdrop) {
        if (wrapper.classList.contains(CLS_MEGA)) {
            closeMega(wrapper, burger, backdrop);
        } else {
            openMega(wrapper, burger, backdrop);
        }
    }

    function openMega(wrapper, burger, backdrop) {
        wrapper.classList.add(CLS_MEGA);
        burger.setAttribute('aria-expanded', 'true');
        burger.setAttribute('aria-label', 'Close menu');

        var mega = wrapper.querySelector(SEL_MEGA);
        if (mega) {
            mega.setAttribute('aria-hidden', 'false');
        }

        track('nav:mega-open', { action: 'open-mega' });
    }

    function closeMega(wrapper, burger, backdrop, silent) {
        wrapper.classList.remove(CLS_MEGA);
        burger.setAttribute('aria-expanded', 'false');
        burger.setAttribute('aria-label', 'Open menu');

        var mega = wrapper.querySelector(SEL_MEGA);
        if (mega) {
            mega.setAttribute('aria-hidden', 'true');
        }

        if (!silent) {
            track('nav:mega-close', { action: 'close-mega' });
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Mobile accordion helpers                                            */
    /* ------------------------------------------------------------------ */
    function toggleAccordion(btn) {
        var item = btn.closest('.gdn-nav__drawer-item');
        if (!item) return;

        var isOpen = item.classList.contains('is-expanded');
        if (isOpen) {
            item.classList.remove('is-expanded');
            btn.setAttribute('aria-expanded', 'false');
            return;
        }

        // allow multiple open (Guardian-like). If you want single-open, call collapseAllAccordions here.
        item.classList.add('is-expanded');
        btn.setAttribute('aria-expanded', 'true');
    }

    function collapseAllAccordions(wrapper) {
        wrapper.querySelectorAll('.gdn-nav__drawer-item.is-expanded').forEach(function (it) {
            it.classList.remove('is-expanded');
        });
        wrapper.querySelectorAll(SEL_EXPAND + '[aria-expanded="true"]').forEach(function (b) {
            b.setAttribute('aria-expanded', 'false');
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Desktop: keyboard navigation for pillar links / subnav              */
    /* ------------------------------------------------------------------ */
    function initDesktopKeyboard(wrapper) {
        /* When a pillar link gains focus on desktop, its subnav shows via CSS
           :focus-within. We only need to handle focus-trap inside the subnav
           and arrow-key navigation between pillars. */

        wrapper.querySelectorAll('.gdn-nav__pillar-link').forEach(function (link) {
            link.addEventListener('keydown', function (e) {
                var item = link.closest('.gdn-nav__pillar-item');

                if (e.key === 'ArrowDown') {
                    e.preventDefault();
                    /* Move focus into subnav */
                    var subnav = item && item.querySelector('.gdn-nav__subnav-link');
                    if (subnav) subnav.focus();
                }

                if (e.key === 'ArrowRight') {
                    e.preventDefault();
                    /* Move to next pillar */
                    var next = item && item.nextElementSibling;
                    var nextLink = next && next.querySelector('.gdn-nav__pillar-link');
                    if (nextLink) nextLink.focus();
                }

                if (e.key === 'ArrowLeft') {
                    e.preventDefault();
                    /* Move to previous pillar */
                    var prev = item && item.previousElementSibling;
                    var prevLink = prev && prev.querySelector('.gdn-nav__pillar-link');
                    if (prevLink) prevLink.focus();
                }
            });
        });

        wrapper.querySelectorAll('.gdn-nav__subnav-link').forEach(function (link) {
            link.addEventListener('keydown', function (e) {
                if (e.key === 'Escape' || e.key === 'ArrowUp') {
                    e.preventDefault();
                    /* Return focus to parent pillar */
                    var pillar = link.closest('.gdn-nav__pillar-item');
                    var pillarLink = pillar && pillar.querySelector('.gdn-nav__pillar-link');
                    if (pillarLink) pillarLink.focus();
                }
                if (e.key === 'ArrowRight') {
                    e.preventDefault();
                    var next = link.parentElement.nextElementSibling;
                    if (next) { var a = next.querySelector('.gdn-nav__subnav-link'); if (a) a.focus(); }
                }
                if (e.key === 'ArrowLeft') {
                    e.preventDefault();
                    var prev = link.parentElement.previousElementSibling;
                    if (prev) { var b = prev.querySelector('.gdn-nav__subnav-link'); if (b) b.focus(); }
                }
            });
        });
    }

    /* ------------------------------------------------------------------ */
    /*  Analytics                                                            */
    /* ------------------------------------------------------------------ */
    function track(name, data) {
        try {
            document.dispatchEvent(new CustomEvent('newspaper:' + name, {
                detail: data, bubbles: true, cancelable: false
            }));
            if (window._satellite) window._satellite.track(name, data);
            if (typeof window.gtag === 'function') {
                window.gtag('event', name.replace(':', '_'), {
                    event_category: 'Navigation', event_label: data.action
                });
            }
        } catch (_) { /* never block rendering */ }
    }

    /* ------------------------------------------------------------------ */
    /*  Run                                                                  */
    /* ------------------------------------------------------------------ */
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    /* SPA: watch for dynamically added nav wrappers */
    if (typeof MutationObserver !== 'undefined') {
        new MutationObserver(function (mutations) {
            mutations.forEach(function (m) {
                m.addedNodes.forEach(function (node) {
                    if (node.nodeType === 1) {
                        if (node.matches && node.matches(SEL_WRAPPER)) {
                            initWrapper(node);
                        } else {
                            node.querySelectorAll && node.querySelectorAll(SEL_WRAPPER)
                                .forEach(initWrapper);
                        }
                    }
                });
            });
        }).observe(document.body, { childList: true, subtree: true });
    }

    window.NewspaperNav = { init: init, version: '3.0.0' };

}());
