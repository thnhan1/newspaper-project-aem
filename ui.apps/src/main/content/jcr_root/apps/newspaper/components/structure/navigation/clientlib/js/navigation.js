/**
 * Newspaper Navigation Component
 * Handles mobile menu toggle, level navigation, and accessibility
 */

(function() {
    'use strict';

    var NAV_SELECTOR = '.cmp-navigation';
    var TOGGLE_SELECTOR = '.cmp-navigation__toggle';
    var CLOSE_SELECTOR = '.cmp-navigation__close';
    var BACK_SELECTOR = '.cmp-navigation__back';
    var BACK_LEVEL2_SELECTOR = '.cmp-navigation__back-level-2';
    var ITEM_TOGGLE_SELECTOR = '.cmp-navigation__item-toggle';
    var LEVEL2_SELECTOR = '.cmp-navigation__level-2-container';
    
    var CLASS_OPEN = 'cmp-navigation--open';
    var CLASS_ACTIVE = 'is-active';
    var CLASS_MENU_OPEN = 'navigation-menu-open';
    
    var currentLevel2 = null;
    var focusTrapElements = [];
    var backdrop = null;

    /**
     * Initialize navigation component
     */
    function init() {
        var navigations = document.querySelectorAll(NAV_SELECTOR);
        
        navigations.forEach(function(nav) {
            initializeNavigation(nav);
        });
    }

    /**
     * Initialize a single navigation instance
     * @param {HTMLElement} nav - Navigation container
     */
    function initializeNavigation(nav) {
        var toggle = document.querySelector(TOGGLE_SELECTOR);
        var closeBtn = nav.querySelector(CLOSE_SELECTOR);
        var itemToggles = nav.querySelectorAll(ITEM_TOGGLE_SELECTOR);
        var backLevel2Btns = nav.querySelectorAll(BACK_LEVEL2_SELECTOR);

        // Mobile toggle (hamburger)
        if (toggle) {
            toggle.addEventListener('click', function() {
                toggleMobileMenu(nav, toggle);
            });
        }

        // Close button
        if (closeBtn) {
            closeBtn.addEventListener('click', function() {
                closeMobileMenu(nav, toggle);
            });
        }

        // Back buttons in Level 2 (one for each Level 2)
        backLevel2Btns.forEach(function(backBtn) {
            backBtn.addEventListener('click', function() {
                goBackToLevel1(nav);
            });
        });

        // Item toggles (Level 1 to Level 2)
        itemToggles.forEach(function(itemToggle) {
            itemToggle.addEventListener('click', function(e) {
                e.preventDefault();
                var targetId = itemToggle.getAttribute('data-target');
                navigateToLevel2(nav, targetId);
            });
        });

        // Keyboard navigation
        setupKeyboardNavigation(nav);

        // Close menu on ESC key
        document.addEventListener('keydown', function(e) {
            if (e.key === 'Escape' && nav.classList.contains(CLASS_OPEN)) {
                closeMobileMenu(nav, toggle);
            }
        });

        // Close menu on window resize to desktop
        var resizeTimeout;
        window.addEventListener('resize', function() {
            clearTimeout(resizeTimeout);
            resizeTimeout = setTimeout(function() {
                if (window.innerWidth >= 1024 && nav.classList.contains(CLASS_OPEN)) {
                    closeMobileMenu(nav, toggle);
                }
            }, 250);
        });
    }

    /**
     * Toggle mobile menu open/close
     */
    function toggleMobileMenu(nav, toggle) {
        var isOpen = nav.classList.contains(CLASS_OPEN);
        
        if (isOpen) {
            closeMobileMenu(nav, toggle);
        } else {
            openMobileMenu(nav, toggle);
        }
    }

    /**
     * Open mobile menu
     */
    function openMobileMenu(nav, toggle) {
        nav.classList.add(CLASS_OPEN);
        toggle.setAttribute('aria-expanded', 'true');
        
        // Create and show backdrop
        createBackdrop(nav, toggle);
        
        // Prevent body scroll
        document.body.classList.add(CLASS_MENU_OPEN);
        document.body.style.overflow = 'hidden';
        
        // Set focus to close button
        var closeBtn = nav.querySelector(CLOSE_SELECTOR);
        if (closeBtn) {
            setTimeout(function() {
                closeBtn.focus();
            }, 300);
        }

        // Setup focus trap
        setupFocusTrap(nav);

        // Fire analytics event
        fireAnalyticsEvent('navigation:open', {
            component: 'navigation',
            action: 'open'
        });
    }

    /**
     * Close mobile menu
     */
    function closeMobileMenu(nav, toggle) {
        nav.classList.remove(CLASS_OPEN);
        toggle.setAttribute('aria-expanded', 'false');
        
        // Remove backdrop
        removeBackdrop();
        
        // Restore body scroll
        document.body.classList.remove(CLASS_MENU_OPEN);
        document.body.style.overflow = '';
        
        // Close any open Level 2
        if (currentLevel2) {
            currentLevel2.classList.remove(CLASS_ACTIVE);
            currentLevel2 = null;
        }

        // Hide back button
        var backBtn = nav.querySelector(BACK_SELECTOR);
        if (backBtn) {
            backBtn.style.display = 'none';
        }
        
        // Return focus to toggle
        if (toggle) {
            toggle.focus();
        }

        // Remove focus trap
        removeFocusTrap();

        // Fire analytics event
        fireAnalyticsEvent('navigation:close', {
            component: 'navigation',
            action: 'close'
        });
    }

    /**
     * Navigate to Level 2 (mobile full-screen)
     */
    function navigateToLevel2(nav, targetId) {
        var level2 = document.getElementById(targetId);
        if (!level2) return;

        currentLevel2 = level2;
        level2.classList.add(CLASS_ACTIVE);

        // Set focus to parent topic link (first item in Level 2)
        var parentLink = level2.querySelector('.cmp-navigation__item-link--parent');
        if (parentLink) {
            setTimeout(function() {
                parentLink.focus();
            }, 300);
        }

        // Fire analytics event
        var parentTitle = level2.getAttribute('data-parent-title');
        fireAnalyticsEvent('navigation:level2', {
            component: 'navigation',
            action: 'navigate',
            level: 2,
            target: parentTitle
        });
    }

    /**
     * Go back from Level 2 to Level 1
     */
    function goBackToLevel1(nav) {
        if (currentLevel2) {
            currentLevel2.classList.remove(CLASS_ACTIVE);
            currentLevel2 = null;
        }

        // Focus first Level 1 item
        var firstLevel1Toggle = nav.querySelector(ITEM_TOGGLE_SELECTOR);
        if (firstLevel1Toggle) {
            setTimeout(function() {
                firstLevel1Toggle.focus();
            }, 300);
        }

        // Fire analytics event
        fireAnalyticsEvent('navigation:back', {
            component: 'navigation',
            action: 'back',
            level: 1
        });
    }

    /**
     * Setup keyboard navigation
     */
    function setupKeyboardNavigation(nav) {
        var focusableElements = nav.querySelectorAll(
            'a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])'
        );

        focusableElements.forEach(function(element, index) {
            element.addEventListener('keydown', function(e) {
                // Arrow key navigation
                if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
                    e.preventDefault();
                    
                    var nextIndex = e.key === 'ArrowDown' ? 
                                    index + 1 : index - 1;
                    
                    if (focusableElements[nextIndex]) {
                        focusableElements[nextIndex].focus();
                    }
                }
            });
        });
    }

    /**
     * Setup focus trap for accessibility
     */
    function setupFocusTrap(nav) {
        focusTrapElements = nav.querySelectorAll(
            'a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])'
        );

        if (focusTrapElements.length === 0) return;

        var firstElement = focusTrapElements[0];
        var lastElement = focusTrapElements[focusTrapElements.length - 1];

        // Trap focus within modal
        document.addEventListener('keydown', handleFocusTrap);

        function handleFocusTrap(e) {
            if (e.key !== 'Tab') return;

            if (e.shiftKey) {
                // Shift + Tab
                if (document.activeElement === firstElement) {
                    e.preventDefault();
                    lastElement.focus();
                }
            } else {
                // Tab
                if (document.activeElement === lastElement) {
                    e.preventDefault();
                    firstElement.focus();
                }
            }
        }

        nav._handleFocusTrap = handleFocusTrap;
    }

    /**
     * Remove focus trap
     */
    function removeFocusTrap() {
        var nav = document.querySelector(NAV_SELECTOR);
        if (nav && nav._handleFocusTrap) {
            document.removeEventListener('keydown', nav._handleFocusTrap);
            delete nav._handleFocusTrap;
        }
        focusTrapElements = [];
    }

    /**
     * Create backdrop overlay
     */
    function createBackdrop(nav, toggle) {
        if (backdrop) return;

        backdrop = document.createElement('div');
        backdrop.className = 'cmp-navigation__backdrop';
        backdrop.setAttribute('aria-hidden', 'true');
        
        // Click backdrop to close menu
        backdrop.addEventListener('click', function() {
            closeMobileMenu(nav, toggle);
        });

        document.body.appendChild(backdrop);
        
        // Trigger animation
        setTimeout(function() {
            backdrop.classList.add('is-visible');
        }, 10);
    }

    /**
     * Remove backdrop overlay
     */
    function removeBackdrop() {
        if (!backdrop) return;

        backdrop.classList.remove('is-visible');
        
        setTimeout(function() {
            if (backdrop && backdrop.parentNode) {
                backdrop.parentNode.removeChild(backdrop);
            }
            backdrop = null;
        }, 300);
    }

    /**
     * Fire analytics event
     */
    function fireAnalyticsEvent(eventName, data) {
        // Custom event for tracking
        var event = new CustomEvent('newspaper:' + eventName, {
            detail: data,
            bubbles: true,
            cancelable: false
        });
        document.dispatchEvent(event);

        // Adobe Analytics
        if (typeof window._satellite !== 'undefined') {
            window._satellite.track(eventName, data);
        }

        // Google Analytics 4
        if (typeof window.gtag === 'function') {
            window.gtag('event', eventName.replace(':', '_'), {
                event_category: 'Navigation',
                event_label: data.action
            });
        }

        // Digital Data Layer
        if (typeof window.digitalData !== 'undefined') {
            window.digitalData.navigation = window.digitalData.navigation || {};
            window.digitalData.navigation.lastAction = data;
        }
    }

    // Initialize on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // Handle dynamic content (SPA support)
    if (typeof MutationObserver !== 'undefined') {
        var observer = new MutationObserver(function(mutations) {
            mutations.forEach(function(mutation) {
                mutation.addedNodes.forEach(function(node) {
                    if (node.nodeType === 1 && node.matches && node.matches(NAV_SELECTOR)) {
                        initializeNavigation(node);
                    }
                });
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
    }

    // Export for testing
    window.NewspaperNavigation = {
        init: init,
        version: '1.0.0'
    };

})();
