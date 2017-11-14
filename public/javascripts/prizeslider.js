var revapi4,
    tpj=jQuery;
tpj(document).ready(function() {
    if(tpj("#rev_slider_4_1").revolution == undefined){
        revslider_showDoubleJqueryError("#rev_slider_4_1");
    }else{
        revapi4 = tpj("#rev_slider_4_1").show().revolution({
            sliderType:"hero",
            sliderLayout:"fullwidth",
            dottedOverlay:"none",
            delay:18000,
            navigation: {
            },
            responsiveLevels:[1240,1024,778,480],
            visibilityLevels:[1240,1024,778,480],
            gridwidth:[1920,1440,778,480],
            gridheight:[650,600,370,310],
            lazyType:"none",
            parallax: {
                type:"scroll",
                origo:"enterpoint",
                speed:400,
                levels:[5,10,15,20,25,30,35,40,45,50,47,48,49,50,51,55]
            },
            shadow:0,
            spinner:"spinner1",
            autoHeight:"on",
            disableProgressBar:"on",
            hideThumbsOnMobile:"off",
            hideSliderAtLimit:0,
            hideCaptionAtLimit:0,
            hideAllCaptionAtLilmit:0,
            debugMode:false,
            fallbacks: {
                simplifyAll:"off",
                disableFocusListener:false
            }
        });
    }
}); /*ready*/