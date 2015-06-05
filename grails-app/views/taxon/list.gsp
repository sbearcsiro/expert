<%@ page import="org.codehaus.groovy.grails.web.json.JSONObject" contentType="text/html;charset=UTF-8" %>
<!DOCTYPE html>
<html>
<head>
  <title>Taxon list</title>
    <meta name="layout" content="main"/>
  <link rel="stylesheet" type="text/css" media="screen" href="${resource(dir:'css',file:'tview.css')}" />
  <link rel="stylesheet" type="text/css" media="screen" href="${resource(dir:'css',file:'colorbox.css')}" />
    <r:require modules="jquery, jqueryui, tooltipster, application, viewer"/>
</head>

<body class="family-list">
<header id="page-header">
    <div class="inner no-top">
        <g:if test="${grailsApplication.config.include.fish.toBoolean()}">
            <hgroup>
                <h1 title="fishmap - find Australian marine fishes"></h1>
            </hgroup>
        </g:if>

        <g:if test="${!grailsApplication.config.include.fish.toBoolean()}">
            <br/>

            <h1 title="${grailsApplication.config.include.appName} - Visual explorer - ${rank} list">${grailsApplication.config.include.appName} - Visual explorer - ${rank} list</h1>
            <br/>
        </g:if>
        <nav id="breadcrumb">
            <ol class="breadcrumb">
                <li><a href="${searchPage}">Search</a> <span class="divider">/</span></li>
                <li class="active">Results by family</li>
            </ol>
        </nav>

        <h2>Results for ${queryDescription ?: 'Australia'}</h2>
    </div>
</header>

<div class="inner no-top">
    <span style="color: grey">Click images to view full size.</span>

    <div id="controls">
        <g:if test="${grailsApplication.config.include.fish.toBoolean()}">
            <label for="sortBy">Sort by:</label>
            <g:select
                    from="[[text: 'Scientific name', id: 'name'], [text: 'Common name', id: 'common'], [text: 'CAAB code', id: 'caabCode']]"
                    name="sortBy" optionKey="id" optionValue="text"/>
        </g:if>
        <g:if test="${!grailsApplication.config.include.fish.toBoolean()}">
            <label for="sortBy">Sort by:</label>
            <g:select from="[[text: 'Scientific name', id: 'name'], [text: 'Common name', id: 'common']]"
                      name="sortBy" optionKey="id" optionValue="text"/>
        </g:if>


            <label for="sortOrder">Sort order:</label>
            <g:select from="['normal','reverse']" name="sortOrder"/>
            <label for="perPage">Results per page:</label>
            <g:select from="[5,10,20,50,100]" name="perPage" value="10"/>
    </div>
        <table class="taxonList">
            <colgroup>
                <col id="tlCheckbox"> <!-- checkbox -->
                <col id="tlName"> <!-- taxon name -->
                <col id="tlImage"> <!-- image -->
                <col id="tlGenera"> <!-- genera -->
            </colgroup>
            <thead>
            <tr><th></th><th>Family name<br/><span style="font-weight: normal;">Common name<br/>

                <g:if test="${grailsApplication.config.include.fish.toBoolean()}">
                    CAAB code
                    <a href="http://www.marine.csiro.au/caab/" class="external">more info</a>
                </g:if>

            </span></th>

                <th style="text-align: center;vertical-align: middle;">Representative image</th>
                <th>Genera</th></tr>
            </thead>
            <tbody>
            <g:each in="${list}" var="i">
                <tr>
                    <!-- checkbox -->
                    <td><input type="checkbox" id="${i.name}" alt="${i.guid}"/></td>
                    <!-- name -->
                    <td><div class="name"><a href="${grailsApplication.config.bie.baseURL}/species/${i.name}"
                                             title="Show ${rank} page">${i.name}</a></div>
                    <!-- common name -->
                        <g:if test="${i.common && i.common.toString() != 'null'}">
                            <div class="common">${i.common}</div>
                        </g:if>
                    <!-- CAAB code -->
                        <g:if test="${i.caabCode}">
                            <div><a href="http://www.marine.csiro.au/caabsearch/caab_search.family_listing?${tv.splitFamilyCaab(caab: i.caabCode)}"
                                    class="external" title="Lookup CAAB code">${i.caabCode}</a></div>
                        </g:if>

                    </td>
                    <!-- image -->
                    <td class="mainImage">
                        <g:set var="largeImageUrl" value="${i.image == JSONObject.NULL ? '' : i.image?.largeImageUrl}"/>
                        <g:set var="imageMetadataUrl"
                               value="${i.image == JSONObject.NULL ? '' : i.image?.imageMetadataUrl}"/>
                        <g:set var="creator" value="${i.image == JSONObject.NULL ? '' : i.image?.creator}"/>
                        <g:set var="license" value="${i.image == JSONObject.NULL ? '' : i.image?.license}"/>
                        <g:set var="rights" value="${i.image == JSONObject.NULL ? '' : i.image?.rights}"/>
                        <g:if test="${largeImageUrl}">
                            <a rel="list" class="imageContainer lightbox" href="#${i.name}-popup">
                                <img class="list" src="${largeImageUrl}" alt title="Click to view full size"/>
                            </a>
                        </g:if>
                        <g:else>
                            <a class="imageContainer no-image" href="#${i.name}-popup">
                                <r:img class="list" uri="/images/no-image.png"/>
                            </a>
                        </g:else>
                        <div style="display: none">
                            <div class="popupContent" id="${i.name}-popup">
                                <img src="${largeImageUrl}" alt/>

                                <div class="details" data-mdurl="${imageMetadataUrl}">
                                    <div class='summary' id="${i.name}-summary">${i.name}</div>

                                    <div><span class="dt">Image by:</span><span class="creator">${creator}</span></div>

                                    <div><span class="dt">License:</span><span class="license">${license}</span></div>

                                    <div style="padding-bottom: 12px;"><span class="dt">Rights:</span><span
                                            class="rights">${rights}</span></div>
                                </div>
                            </div>
                        </div>
                    </td>
                    <!-- genera -->
                    <td class="genera">
                        <table class="genera">
                            <g:each in="${i.genera}" var="g" status="count">
                                <g:if test="${count % 4 == 0}">
                                    <tr>
                                </g:if>
                                <td>
                                    <g:if test="${g.image}">
                                        <a rel="${i.name}" class="imageContainer lightbox" href="#${g.name}-popup">
                                            <img class="thumb" src="${g.image?.largeImageUrl}"/>
                                        </a>
                                    </g:if>
                                    <g:else>
                                        <r:img class="no-image-small" uri="/images/no-image-small.png"/>
                                    </g:else>
                                    <g:link action="species" params="[key: key, genus: g.name]"
                                            title="${g.speciesCount} species">
                                        <span class="${(g.name.size() > 13 ? 'tight' : '') + (g.name.size() > 17 ? ' veryTight' : '')}">${g.name}</span>
                                    </g:link>
                                    <g:if test="${g.image}">
                                        <div style="display: none">
                                            <div class="popupContent" id="${g.name}-popup">
                                                <img src="${g.image?.largeImageUrl}" alt/>

                                                <div class="details" data-mdurl="${g.image?.imageMetadataUrl}">
                                                    <div class="summary" id="${g.name}-summary">${g.name}</div>

                                                    <div><span class="dt">Image by:</span><span
                                                            class="creator">${g.image?.creator}</span></div>

                                                    <div><span class="dt">License:</span><span
                                                            class="license">${g.image?.license}</span></div>

                                                    <div style="padding-bottom: 12px;"><span
                                                            class="dt">Rights:</span><span
                                                            class="rights">${g.image?.rights}</span></div>
                                                </div>
                                            </div>
                                        </div>
                                    </g:if>
                                </td>
                                <g:if test="${count % 4 == 3 || count == i.genera.size()}">
                                    </tr>
                                </g:if>
                            </g:each>
                        </table>
                    </td>
                </tr>
            </g:each>
            </tbody>
        </table>
        <section id="pagination">
            <tv:paginate start="${start}" pageSize="${pageSize}" total="${total}"
                         params="${[key: key, sortBy: sortBy, sortOrder: sortOrder]}"/>
            <p id="viewLinks">
                Total <tv:pluraliseRank rank="${rank}"/>: ${total}
                <span class="link" id="speciesList">Show species list for checked <tv:pluraliseRank
                        rank="${rank}"/></span>
                <g:link style="padding-left:20px;" action="species"
                        params="[key: key]">Show all results by species</g:link>
                <g:link style="padding-left:20px;" action="data"
                        params="[key: key]">Show data table for all species</g:link><br/>
            </p>

            <div id="checkboxButtons">
                <button id="selectAll" type="button">Select all</button>
                <button id="clearAll" type="button">Clear all</button>
            </div>
        </section>
    </div>


<script>
    var serverUrl = "${grailsApplication.config.grails.serverURL}";

        $(document).ready(function () {
            // wire link to species list
            $('#speciesList,#speciesData').click(function () {
                // collect the selected ranks
                var checked = "", which = 'species';
                $('input[type="checkbox"]:checked').each(function () {
                    checked += (checked === "" ? '' : ',') + $(this).attr('id');
                });
                if (checked === "") {
                    alert("No families selected");
                }
                else {
                    if (this.id === 'speciesData') {
                        which = 'data'
                    }
                    document.location.href = "${grailsApplication.config.grails.serverURL}" +
                    "/taxon/" + which + "?taxa=" + checked + "&key=${key}";
                }
            });

//            // fix some heights
//            var rows = $('table.taxonList > tbody > tr');
//
//            rows.each(function(i, row) {
//                var jRow = $(row);
//                var max = getMaxOfArray(jRow.find('td.genera table.genera td img.thumb').map(function (i, e) { return e.naturalHeight }));
//                if (max > 350) max = 350;
//                if (max > 0) jRow.find('td.mainImage > a').css('height',max+'px');
//            });
//
//            function getMaxOfArray(numArray) {
//                return Math.max.apply(null, numArray);
//            }

            tviewer.init(serverUrl);
        });
    </script>
</body>
</html>