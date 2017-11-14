(function($, window, document, undefined) {
    var faqNotification = " of ";

    var gridContainer = $('.grid-container');


    /*******************************
     init cubeportfolio
     ****************************** */
    gridContainer.each(function(){
        var $this = $(this);

        $this.cubeportfolio({
            layoutMode:$this.data('layoutmode'),
            rewindNav: true,
            scrollByPage: false,
            filters: $this.data('filter'),
            mediaQueries: [{

                width:1600,
                cols:($this.data('col-num-1600'))? $this.data('col-num-1600'): 5
            },{
                width:1200,
                cols:($this.data('col-num-1200'))? $this.data('col-num-1200'): 4
            },{
                width: 900,
                cols:($this.data('col-num-900'))? $this.data('col-num-900'): 3
            }, {
                width: 600,
                cols:($this.data('col-num-600')) ? $this.data('col-num-600'): 2
            }, {
                width: 480,
                cols:($this.data('col-num-480'))? $this.data('col-num-480'): 1
            }, {
                width: 300,
                cols:($this.data('col-num-320'))? $this.data('col-num-320'): 1
            }],
            defaultFilter: '*',
            animationType: $this.data('animate-type'),
            gapHorizontal: ($this.data('gap-horizontal')) ? $this.data('gap-horizontal') : 0 ,
            gapVertical: ($this.data('gap-vertical')) ? $this.data('gap-vertical') : 0 ,
            gridAdjustment:$this.data('grid-adjusment'),
            caption: '',
            displayType: 'sequentially',
            displayTypeSpeed: 100,

            // lightbox
            lightboxDelegate: '.cbp-lightbox',
            lightboxGallery: true,
            lightboxTitleSrc: 'data-title',
            lightboxCounter: '<div class="cbp-popup-lightbox-counter">{{current}}'  + faqNotification+  '{{total}}</div>',

            // singlePage popup
            singlePageDelegate: '.cbp-singlePage',
            singlePageDeeplinking: true,
            singlePageAnimation :'left',
            singlePageStickyNavigation: true,
            singlePageCounter: '<div class="cbp-popup-singlePage-counter">{{current}}'  + faqNotification +  '{{total}}</div>',
            singlePageCallback: function(url, element) {

                // to update singlePage content use the following method: this.updateSinglePage(yourContent)
                var t = this;


                $.ajax({
                        url: url,
                        type: 'GET',
                        dataType: 'html',
                        timeout: 5000
                    })
                    .done(function(result) {
                        t.updateSinglePage(result);
                        setTimeout(function(){
                            $(".directToHome").click(function(){
                                $('.cbp-popup-close').click();
                            });
                        }, 800);
                    })
                    .fail(function() {
                        t.updateSinglePage("Error! Please refresh the page!");
                    });
            },

            // singlePageInline
            singlePageInlineDelegate: '.cbp-singlePageInline',
            singlePageInlinePosition: $this.data('page-position'),
            singlePageInlineInFocus: true,
            singlePageInlineCallback: function(url, element) {
                // to update singlePageInline content use the following method: this.updateSinglePageInline(yourContent)
                var t = this;

                $.ajax({
                        url: url,
                        type: 'GET',
                        dataType: 'html',
                        timeout: 5000
                    })
                    .done(function(result) {

                        t.updateSinglePageInline(result);

                    })
                    .fail(function() {
                        t.updateSinglePageInline("Error! Please refresh the page!");
                    });
            }
        });
    });
})(jQuery, window, document);