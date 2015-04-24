modules = {
    application {
        dependsOn 'jquery, jqueryui, html5'
        //defaultBundle false
        resource url: 'js/combobox.js'
    }

    viewer {
        resource url: 'js/tviewer.js'
        resource url: 'css/tview.css'
        resource url: 'css/colorbox.css'
    }

    jquery {
        resource url: 'js/jquery-1.10.2.min.js'
    }

    map {
        resource url: 'js/jquery.ba-bbq.min.js'
        resource url: 'js/expert.js'
        resource url: 'js/selection-map.js'
        resource url: 'js/wms.js'
        resource url: 'js/keydragzoom.js'
    }

    jqueryui {
        resource url: 'js/jquery-ui-1.11.1.min.js'
        resource url: 'js/jquery.ba-bbq.min.js'
        resource url: 'js/jquery.colorbox-min.js'
    }

    tooltipster {
        dependsOn 'jquery'
        resource url:'css/tooltipster.css'
        resource url: 'js/jquery.tooltipster.min.js'
    }

    html5 {
        resource url:'js/html5.js',
                wrapper: { s -> "<!--[if lt IE 9]>$s<![endif]-->" }
    }

}