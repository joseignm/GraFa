<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
  <head>
    <meta charset="UTF-8">
    <title>Facet</title>
  </head>

  <body>

  <script>
      function getInstances() {
          var request = new XMLHttpRequest();
          request.onreadystatechange = function() {
              if(request.readyState==4 && request.status==200) {
                  document.getElementById("instances").innerHTML = request.responseText;
              }
          };
          var input = document.getElementById("ins-input");
          request.open("GET", "instances?keyword="+input.value, true);
          request.send();
      }

      function getQ() {
          var input = document.getElementById('ins-input');
          var list = input.getAttribute('list');
          var options = document.querySelectorAll('#' + list + ' option');
          var hiddenInput = document.getElementById('instance-hidden');
          var inputValue = input.value;

          for(var i = 0; i < options.length; i++) {
              var option = options[i];
              if(option.innerText === inputValue) {
                  hiddenInput.value = option.getAttribute('code');
                  return true;
              }
          }
          return true;
      }
  </script>

  <h1>Search</h1>
  <br>
  <form action="search" method="get" onsubmit="return getQ()">
      <datalist id="instances"></datalist>
      Keyword: <input type="text" name="keyword"><br>
      Type: <input id="ins-input" list="instances" oninput="getInstances()"><br>
      <input type="hidden" name="instance" id="instance-hidden">
      <input type="submit" value="Search">
  </form>
  </body>
</html>
