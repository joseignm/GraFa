<%@ page import="java.util.List" %>
<%@ page import="cl.uchile.dcc.facet.web.Entry" %>
<%@ page import="cl.uchile.dcc.facet.web.CodeNameValue" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>

<head>
    <meta charset="utf-8">
    <% String lang = request.getParameter("lang");
        if(lang==null) lang = "en"; %>
    <% if(lang.equals("es")) { %>
    <title>GraFa - Resultados</title>
    <% } else { %>
    <title>GraFa - Results</title>
    <% } %>

    <link rel="shortcut icon" href="css/favicon.ico" type="image/x-icon">
    <link rel="icon" href="css/favicon.ico" type="image/x-icon">

    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">
    <link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/dojo/1.12.1/dijit/themes/claro/claro.css">

    <!-- Bootstrap CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css" integrity="sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp" crossorigin="anonymous">

    <link rel="stylesheet" href="css/styles.css">

    <!-- Bootstrap JS -->
    <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>

    <script>
        dojoConfig = {async: true}
    </script>
    <script src="https://ajax.googleapis.com/ajax/libs/dojo/1.12.1/dojo/dojo.js"></script>

</head>

<body class="claro">

<script>
    function showPropertyValues(caller) {
        var keyword = document.getElementById("keyword")==null ? "" : document.getElementById("keyword").value;
        var lang = document.getElementById("lang")==null ? "" : document.getElementById("lang").value;
        var instance = document.getElementById("instance").value;
        var property = caller.value;
        var selectedArray = document.getElementsByName("properties");
        var request = new XMLHttpRequest();
        request.onreadystatechange = function() {
            if(request.readyState==4 && request.status==200) {
                require(["dojo/store/Memory", "dijit/form/FilteringSelect"], function (Memory, FilteringSelect) {
                    var response = JSON.parse(request.responseText);
                    var data = new Memory({data : response});
                    new FilteringSelect({
                        id : property,
                        name : "properties",
                        store : data,
                        searchAttr : "name",
                        required: false
                    }, property);
                    var submitButton = document.getElementById(property+"-btn");
                    submitButton.style.display = "inline";
                    submitButton.classList = "btn btn-default btn-xs";
                })
            }
        };
        var url = "properties?keyword="+keyword+"&instance="+instance+"&property="+property;
        if(lang !== "") {
            url = url + "&lang=" + lang;
        }
        for(var i=0; i<selectedArray.length; i++) {
            if(selectedArray[i].type == "hidden") {
                url = url + "&selected=" + encodeURIComponent(selectedArray[i].value);
            }
        }
        var emptyDiv = document.getElementById(property);
        <% if(lang.equals("es")) { %>
        emptyDiv.innerHTML = "<i class='fa fa-circle-o-notch fa-spin'></i> Cargando...";
        <% } else { %>
        emptyDiv.innerHTML = "<i class='fa fa-circle-o-notch fa-spin'></i> Loading...";
        <% } %>
        request.open("GET", url, true);
        request.send();
    }

    function removeProperty(caller) {
        var property = caller.value;
        document.getElementById(property).disabled = true;
        document.getElementById("form").submit();
    }

    function changeLanguage(code) {
        var newSearch = location.search;
        if(newSearch.indexOf("?") === -1) {
            newSearch = "?lang=" + code;
        } else if(newSearch.match(/lang=[^&$]*/i)) {
            newSearch = newSearch.replace(/lang=[^&$]*/i, 'lang='+code)
        } else {
            newSearch += "&lang=" + code;
        }
        location.search = newSearch;
    }
</script>

<% if(request.getAttribute("results")==null) { %>
<div class="alert alert-danger">
    <% if(lang.equals("es")) { %>
    <strong>Error</strong> La consulta causó una excepción.
    <% } else { %>
    <strong>Error!</strong> Query triggered an exception.
    <% } %>
</div>
<% } else {
    List<Entry> entries = (List<Entry>) request.getAttribute("results");
    List<CodeNameValue> properties = (List<CodeNameValue>) request.getAttribute("properties");
    List<String> checkedProperties = (List<String>) request.getAttribute("checked");
    List<CodeNameValue> labelsProperties = (List<CodeNameValue>) request.getAttribute("labels");
%>

<div class="container-fluid">
<div class="row">
    <div class="col-md-3">
        <a href="${pageContext.request.contextPath}/?lang=<%=lang%>">
            <img src="css/logoBM.png" class="pull-left image-margin" style="width: 200px; height: 99px">
        </a>
    </div>
    <div class="col-md-7">
        <% if(lang.equals("es")) { %>
        <h1>Resultados</h1>
        <% } else { %>
        <h1>Results</h1>
        <% } %>
    </div>
    <div class="col-md-2 text-right">
        <div class="dropdown-right">
            <button aria-expanded="false" data-toggle="dropdown" type="button" class="btn btn-default dropdown-toggle"><span class="glyphicon glyphicon-globe"></span> Language
                <span class="caret"></span></button>
            <ul class="dropdown-menu dropdown-menu-right">
                <li><a href="#" onclick="changeLanguage('en')">English</a></li>
                <li><a href="#" onclick="changeLanguage('es')">Español</a></li>
            </ul>
        </div>
    </div>
</div>
<div class="row">
<div class="col-md-3">
    <% if(lang.equals("es")) { %>
    <h4>Buscando por:</h4>
    <% } else { %>
    <h4>Current Query:</h4>
    <% } %>

<form id="form" action="search" method="get">
<% if(request.getParameter("keyword")!=null && !request.getParameter("keyword").isEmpty()) {%>
    <div class="form-group">
        <% if(lang.equals("es")) { %>
        <label for="keyword">Palabra clave:</label>
        <% } else { %>
        <label for="keyword">Keyword:</label>
        <% } %>
        <input readonly type="text" class="form-control input-sm" id="keyword" name="keyword" value="<%= request.getParameter("keyword").trim() %>">
    </div>
<% } %>

<% if(request.getParameter("lang")!=null && !request.getParameter("lang").isEmpty()) {%>
    <input readonly type="hidden" id="lang" name="lang" value="<%= request.getParameter("lang").trim() %>">
<% } %>

<% if(request.getAttribute("type")!=null) {%>
    <div class="form-group">
        <% if(lang.equals("es")) { %>
        <label for="ins-input">Tipo:</label>
        <% } else { %>
        <label for="ins-input">Type:</label>
        <% } %>
        <input readonly type="text" class="form-control input-sm" id="ins-input" value="<%= request.getAttribute("type").toString().trim() %>">
    </div>

<% } %>
    <input readonly type="hidden" id="instance" name="instance" value="<%= request.getParameter("instance")==null ? "" : request.getParameter("instance")%>">

    <% if(checkedProperties != null) { %>
        <% for(int i = 0; i < checkedProperties.size(); i++) { %>
        <div class="form-group">
            <input id="<%=checkedProperties.get(i)%>" readonly type="hidden" name="properties" value="<%=checkedProperties.get(i)%>">
            <label for="<%=checkedProperties.get(i)%>-vis"><%=labelsProperties.get(i).getCode()%></label>
            <div class="input-group">
                <input id="<%=checkedProperties.get(i)%>-vis" readonly type="text" class="form-control input-sm" value="<%=labelsProperties.get(i).getName()%>">
                <span class="input-group-btn">
                <button type="button" class="btn btn-default btn-sm" value="<%=checkedProperties.get(i)%>" onclick="removeProperty(this)">
                    <span class="glyphicon glyphicon-remove"></span>
                </button>
                </span>
            </div>
        </div>
        <% } %>
    <% } %>

    <br>
    <% if(lang.equals("es")) { %>
    <h4>Propiedades:</h4>
    <% } else { %>
    <h4>Properties:</h4>
    <% } %>
    <% for(CodeNameValue property : properties) {%>
        <button type="button" class="btn btn-default btn-xs" value="<%= property.getCode() %>" onclick="showPropertyValues(this)">
            <span class="glyphicon glyphicon-plus"></span>
        </button>
        <%= property.getName()+" ("+property.getValue()+" "%>
    <% if(lang.equals("es")) { %>
    <%="resultados)"%>
    <% } else { %>
    <%="results)"%>
    <% } %>
        <br>
        <div id="<%=property.getCode()%>"></div>
        <button type="submit" id="<%=property.getCode()%>-btn" hidden><span class="glyphicon glyphicon-search"></span></button>
        <br>
    <% } %>
    </form>
</div>
<div class="col-md-9">
    <% if(lang.equals("es")) { %>
    <h5>Coincidencias totales: <%= request.getAttribute("total") %> </h5>
    <h5>Mostrando primeros <%= entries.size() %> resultados</h5>
    <% } else { %>
    <h5>Matching documents: <%= request.getAttribute("total") %> </h5>
    <h5>Showing top <%= entries.size() %> results</h5>
    <% } %>
    <% for(Entry entry : entries) {%>
    <div class="panel panel-default" style="margin-top: 10px; margin-bottom: 10px">
        <div class="panel-body">
        <% if(entry.getImage() != null) {%>
            <img src="<%= entry.getImage()%>" class="pull-right image-margin image-entry">
        <% } %>
            <h3><a href="http://www.wikidata.org/wiki/<%=entry.getSubject()%>"><%=entry.getLabel()%></a> <small><%=entry.getAltLabels()%></small></h3>
            <p><%=entry.getDescription()%></p>
        </div>
    </div>
    <% } %>
</div>
</div>
<% } %>
</body>
</html>
