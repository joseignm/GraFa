<%@ page import="java.util.List" %>
<%@ page import="cl.uchile.dcc.facet.web.Entry" %>
<%@ page import="cl.uchile.dcc.facet.web.CodeNameValue" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>Search Results</title>
</head>
<body>

<script>
    function showPropertyValues(caller) {
        var keyword = document.getElementById("keyword").value;
        var instance = document.getElementById("instance").value;
        var property = caller.value;
        var selectedArray = document.getElementsByName("properties");
        var request = new XMLHttpRequest();
        request.onreadystatechange = function() {
            if(request.readyState==4 && request.status==200) {
                document.getElementById(property).innerHTML = request.responseText;
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
</script>

<% if(request.getAttribute("results")==null) { %>
<h1> Error performing query </h1>
<% } else {
    List<Entry> entries = (List<Entry>) request.getAttribute("results");
    List<CodeNameValue> properties = (List<CodeNameValue>) request.getAttribute("properties");
    List<String> checkedProperties = (List<String>) request.getAttribute("checked");
%>

<h1>Results</h1>
<h3>Current Query</h3>

<form action="search" method="post">
    Keyword: <input readonly type="text" id="keyword" name="keyword" value="
<%= request.getParameter("keyword")==null ? "" : request.getParameter("keyword") %>"><br>
    Type: <input readonly type="text" name="ins-input" value="
<%= request.getAttribute("type")==null ? "" : request.getAttribute("type").toString()%>">
    <input readonly type="hidden" id="instance" name="instance" value="
<%= request.getParameter("instance")==null ? "" : request.getParameter("instance")%>"><br><br>

    <% if(checkedProperties != null) { %>
        <% for(String checked : checkedProperties) { %>
            <input readonly type="hidden" name="properties" value="<%=checked%>">
        <% } %>
    <% } %>

    Properties: <br><div>
    <% for(CodeNameValue property : properties) {%>
        <button type="button" value="<%= property.getCode() %>" onclick="showPropertyValues(this)">+</button>
        <%= property.getName()+" ("+property.getValue()+")" %>
        <br>
        <div id="<%=property.getCode()%>"></div><br>
    <% } %>
    <input type="submit" value="Refine search">
</div>
</form>

<p>Matching documents: <%= request.getAttribute("total") %> </p>
<p>Showing top <%= entries.size() %> results.</p>

<table>
    <tr>
        <th>IRI</th>
        <th>Score</th>
        <th>Name</th>
        <th>Description</th>
        <th>Alternative Names</th>
    </tr>
    <% for(Entry entry : entries) {%>
        <tr>
            <td> <a href="http://www.wikidata.org/wiki/<%= entry.getSubject() %>">
                <%= entry.getSubject() %>
            </a></td>
            <td> <%= entry.getBoosts() %> </td>
            <td> <%= entry.getLabel() %></td>
            <td> <%= entry.getDescription() %></td>
            <td> <%= entry.getAltLabels() %></td>
        </tr>
    <% } %>
</table>
<% } %>

</body>
</html>
