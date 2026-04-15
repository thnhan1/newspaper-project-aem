/**
 * Newspaper Breadcrumb Component JavaScript
 * AEM 6.5.24 - Vanilla JavaScript (ES6+)
 * 
 * Features:
 * - Analytics tracking for breadcrumb clicks
 * - Keyboard navigation enhancements
 * - Accessibility improvements
 */

(function() {
    'use strict';

    var BREADCRUMB_SELECTOR = '.cmp-breadcrumb';
    var BREADCRUMB_LINK_SELECTOR = '.cmp-breadcrumb__item-link';
    
    /**
     * Initialize breadcrumb component
     */
    function init() {
        var breadcrumbs = document.querySelectorAll(BREADCRUMB_SELECTOR);
        
        breadcrumbs.forEach(function(breadcrumb) {
            initializeBreadcrumb(breadcrumb);
        });
    }

    /**
     * Initialize a single breadcrumb component
     * @param {HTMLElement} breadcrumb - The breadcrumb container element
     */
    function initializeBreadcrumb(breadcrumb) {
        var links = breadcrumb.querySelectorAll(BREADCRUMB_LINK_SELECTOR);
        
        links.forEach(function(link, index) {
            // Add analytics tracking
            link.addEventListener('click', function(event) {
                trackBreadcrumbClick(link, index, breadcrumb);
            });

            // Enhanced keyboard navigation
            link.addEventListener('keydown', function(event) {
                handleKeyboardNavigation(event, link, links, index);
            });

            // Add accessibility attributes
            enhanceAccessibility(link, index, links.length);
        });

        // Truncate long breadcrumbs on mobile if needed
        handleTruncation(breadcrumb);
    }

    /**
     * Track breadcrumb click for analytics
     * @param {HTMLElement} link - The clicked link element
     * @param {number} index - The index of the breadcrumb item
     * @param {HTMLElement} breadcrumb - The breadcrumb container
     */
    function trackBreadcrumbClick(link, index, breadcrumb) {
        var breadcrumbData = {
            component: 'breadcrumb',
            action: 'click',
            label: link.textContent.trim(),
            position: index + 1,
            url: link.href,
            timestamp: new Date().toISOString()
        };

        // Fire custom event for analytics tracking
        var event = new CustomEvent('newspaper:breadcrumb:click', {
            detail: breadcrumbData,
            bubbles: true,
            cancelable: false
        });
        breadcrumb.dispatchEvent(event);

        // Adobe Analytics / Google Analytics integration
        if (typeof window.digitalData !== 'undefined') {
            window.digitalData = window.digitalData || {};
            window.digitalData.breadcrumb = window.digitalData.breadcrumb || {};
            window.digitalData.breadcrumb.lastClick = breadcrumbData;
        }

        // Google Analytics 4 (if available)
        if (typeof window.gtag === 'function') {
            window.gtag('event', 'breadcrumb_click', {
                event_category: 'Navigation',
                event_label: breadcrumbData.label,
                breadcrumb_position: breadcrumbData.position
            });
        }

        // Adobe Analytics (if available)
        if (typeof window._satellite !== 'undefined') {
            window._satellite.track('breadcrumb_click', breadcrumbData);
        }
    }

    /**
     * Handle keyboard navigation between breadcrumb items
     * @param {KeyboardEvent} event - The keyboard event
     * @param {HTMLElement} currentLink - The current link element
     * @param {NodeList} allLinks - All breadcrumb links
     * @param {number} currentIndex - The index of current link
     */
    function handleKeyboardNavigation(event, currentLink, allLinks, currentIndex) {
        var handled = false;

        switch(event.key) {
            case 'ArrowRight':
                // Move to next breadcrumb item
                if (currentIndex < allLinks.length - 1) {
                    allLinks[currentIndex + 1].focus();
                    handled = true;
                }
                break;
            
            case 'ArrowLeft':
                // Move to previous breadcrumb item
                if (currentIndex > 0) {
                    allLinks[currentIndex - 1].focus();
                    handled = true;
                }
                break;
            
            case 'Home':
                // Move to first breadcrumb item
                allLinks[0].focus();
                handled = true;
                break;
            
            case 'End':
                // Move to last breadcrumb item
                allLinks[allLinks.length - 1].focus();
                handled = true;
                break;
        }

        if (handled) {
            event.preventDefault();
        }
    }

    /**
     * Enhance accessibility attributes
     * @param {HTMLElement} link - The link element
     * @param {number} index - The index of the breadcrumb item
     * @param {number} total - Total number of breadcrumb items
     */
    function enhanceAccessibility(link, index, total) {
        // Add aria-label for screen readers
        var linkText = link.textContent.trim();
        var position = index + 1;
        
        link.setAttribute('aria-label', linkText + ' - Breadcrumb item ' + position + ' of ' + total);
        
        // Add keyboard hint
        if (index === 0) {
            link.setAttribute('data-keyboard-hint', 'Use arrow keys to navigate breadcrumbs');
        }
    }

    /**
     * Handle breadcrumb truncation on mobile devices
     * @param {HTMLElement} breadcrumb - The breadcrumb container
     */
    function handleTruncation(breadcrumb) {
        var items = breadcrumb.querySelectorAll('.cmp-breadcrumb__item');
        
        // Show truncation indicator if there are many items on mobile
        if (items.length > 4 && window.innerWidth < 768) {
            items.forEach(function(item, index) {
                // Keep first and last two items visible, truncate middle
                if (index > 0 && index < items.length - 2) {
                    var link = item.querySelector(BREADCRUMB_LINK_SELECTOR);
                    if (link) {
                        link.setAttribute('title', link.textContent.trim());
                    }
                }
            });
        }
    }

    /**
     * Handle window resize for responsive behavior
     */
    var resizeTimeout;
    function handleResize() {
        clearTimeout(resizeTimeout);
        resizeTimeout = setTimeout(function() {
            var breadcrumbs = document.querySelectorAll(BREADCRUMB_SELECTOR);
            breadcrumbs.forEach(function(breadcrumb) {
                handleTruncation(breadcrumb);
            });
        }, 250);
    }

    // Initialize on DOM ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    // Handle dynamic content loading (for SPA/AJAX scenarios)
    if (typeof MutationObserver !== 'undefined') {
        var observer = new MutationObserver(function(mutations) {
            mutations.forEach(function(mutation) {
                mutation.addedNodes.forEach(function(node) {
                    if (node.nodeType === 1) { // Element node
                        if (node.matches && node.matches(BREADCRUMB_SELECTOR)) {
                            initializeBreadcrumb(node);
                        } else if (node.querySelector) {
                            var breadcrumbs = node.querySelectorAll(BREADCRUMB_SELECTOR);
                            breadcrumbs.forEach(initializeBreadcrumb);
                        }
                    }
                });
            });
        });

        observer.observe(document.body, {
            childList: true,
            subtree: true
        });
    }

    // Handle window resize
    window.addEventListener('resize', handleResize);

    // Export for testing/debugging (optional)
    window.NewspaperBreadcrumb = {
        init: init,
        version: '1.0.0'
    };

})();
