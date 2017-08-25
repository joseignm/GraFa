<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
  <head>
      <meta charset="UTF-8">
      <title>Facet</title>

      <!-- Bootstrap CSS -->
      <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">
      <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css" integrity="sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp" crossorigin="anonymous">

      <!-- Bootstrap JS -->
      <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
      <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>

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

  <div class="container">

      <div class="row">
          <div class="col-md-6 col-md-offset-3 text-center">
              <h1>Faceted Navegation</h1>
              <h3>Search</h3>
          </div>
      </div>

      <br><br>

      <div class="row">
          <div class="col-md-6 col-md-offset-3">
              <form action="search" method="get" onsubmit="return getQ()">
                  <datalist id="instances"></datalist>

                  <div class="form-group">
                      <label for="keyword">Keyword:</label>
                      <input id="keyword" class="form-control" type="text" name="keyword">
                  </div>

                  <div class="form-group">
                      <label for="ins-input">Type:</label>
                      <input id="ins-input" class="form-control" list="instances" oninput="getInstances()">
                      <input type="hidden" name="instance" id="instance-hidden">
                  </div>

                  <input type="submit" value="Search" class="btn btn-default">
              </form>
          </div>
      </div>
  </div>
  </body>
</html>
