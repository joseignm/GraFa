<%@ page import="java.util.List" %>
<%@ page import="cl.uchile.dcc.facet.web.Entry" %>
<%@ page import="cl.uchile.dcc.facet.web.CodeNameValue" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>

<head>
    <meta charset="utf-8">
    <title>Search Results</title>

    <link rel="stylesheet" href="http://ajax.googleapis.com/ajax/libs/dojo/1.12.1/dijit/themes/claro/claro.css">

    <!-- Bootstrap CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css" integrity="sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp" crossorigin="anonymous">

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
                        searchAttr : "name"
                    }, property);
                    var submitButton = document.getElementById(property+"-btn");
                    submitButton.style.display = "inline";
                    submitButton.classList = "btn btn-default btn-xs";
                })
            }
        };
        var url = "properties?keyword="+keyword+"&instance="+instance+"&property="+property;
        for(var i=0; i<selectedArray.length; i++) {
            if(selectedArray[i].type == "hidden") {
                url = url + "&selected=" + encodeURIComponent(selectedArray[i].value);
            }
        }
        request.open("GET", url, true);
        request.send();
    }

    function removeProperty(caller) {
        var property = caller.value;
        document.getElementById(property).disabled = true;
        document.getElementById("form").submit();
    }
</script>

<% if(request.getAttribute("results")==null) { %>
<div class="alert alert-danger"> <strong>Error!</strong> Query triggered an exception. </div>
<% } else {
    List<Entry> entries = (List<Entry>) request.getAttribute("results");
    List<CodeNameValue> properties = (List<CodeNameValue>) request.getAttribute("properties");
    List<String> checkedProperties = (List<String>) request.getAttribute("checked");
    List<CodeNameValue> labelsProperties = (List<CodeNameValue>) request.getAttribute("labels");
%>

<div class="container-fluid">
<div class="row">
    <div class="col-md-12"> <h1>Results</h1> </div>
</div>
<div class="row">
<div class="col-md-3">
<h4>Current Query:</h4>

<form id="form" action="search" method="get">
<% if(request.getParameter("keyword")!=null && !request.getParameter("keyword").isEmpty()) {%>
    <div class="form-group">
        <label for="keyword">Keyword:</label>
        <input readonly type="text" class="form-control input-sm" id="keyword" name="keyword" value="<%= request.getParameter("keyword").trim() %>">
    </div>
<% } %>

<% if(request.getAttribute("type")!=null) {%>
    <div class="form-group">
        <label for="ins-input">Type:</label>
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

    <h4>Properties:</h4>
    <% for(CodeNameValue property : properties) {%>
        <button type="button" class="btn btn-default btn-xs" value="<%= property.getCode() %>" onclick="showPropertyValues(this)">
            <span class="glyphicon glyphicon-plus"></span>
        </button>
        <%= property.getName()+" ("+property.getValue()+")" %>
        <br>
        <div id="<%=property.getCode()%>"></div>
        <button type="submit" id="<%=property.getCode()%>-btn" hidden><span class="glyphicon glyphicon-search"></span></button>
        <br>
    <% } %>
    </form>
</div>
<div class="col-md-9">
    <h5>Matching documents: <%= request.getAttribute("total") %> </h5>
    <h5>Showing top <%= entries.size() %> results</h5>

    <table class="table table-hover">
        <thead><tr>
            <th class="col-md-2">Name</th>
            <th class="col-md-4">Alternative Names</th>
            <th class="col-md-6">Description</th>
        </tr></thead>
        <tbody>
        <% for(Entry entry : entries) {%>
            <tr>
                <td> <a href="http://www.wikidata.org/wiki/<%= entry.getSubject() %>">
                    <%= entry.getLabel() %>
                </a></td>
                <td> <%= entry.getAltLabels() %></td>
                <td> <%= entry.getDescription() %></td>
            </tr>
        <% } %>
        </tbody>
    </table>
</div>
</div>
</div>
<% } %>
</body>
</html>
