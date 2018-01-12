<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
  <head>
      <meta charset="UTF-8">
      <title>GraFa</title>

      <link rel="shortcut icon" href="css/favicon.ico" type="image/x-icon">
      <link rel="icon" href="css/favicon.ico" type="image/x-icon">

      <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css">

      <!-- Bootstrap CSS -->
      <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" integrity="sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" crossorigin="anonymous">
      <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap-theme.min.css" integrity="sha384-rHyoN1iRsVXV4nD0JutlnGaslCJuC7uwjduW9SVrLvRYooPp2bWYgmgJQIXwl/Sp" crossorigin="anonymous">

      <link rel="stylesheet" href="css/styles.css">

      <!-- Bootstrap JS -->
      <script src="https://ajax.googleapis.com/ajax/libs/jquery/3.2.1/jquery.min.js"></script>
      <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" integrity="sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" crossorigin="anonymous"></script>

  </head>

  <body>

  <script>
      function getInstances() {
          var loadIcon = document.getElementById("load-icon");
          loadIcon.style.display = "inline-block";
          var request = new XMLHttpRequest();
          request.onreadystatechange = function() {
              if(request.readyState==4 && request.status==200) {
                  document.getElementById("instances").innerHTML = request.responseText;
                  var loadIcon = document.getElementById("load-icon");
                  loadIcon.style.display = "none";
              }
          };
          var input = document.getElementById("ins-input");
          var lang = document.getElementById("lang")==null ? "" : document.getElementById("lang").value;
          var url = "instances?keyword="+input.value;
          if(lang !== "") {
              url = url + "&lang=" + lang;
          }
          request.open("GET", url, true);
          request.send();
      }

      function inputCallback(input, callback, delay) {
          var timer = null;
          input.onkeyup = function() {
              if(timer) window.clearTimeout(timer);
              timer = window.setTimeout(function() {
                  timer = null;
                  callback();
              }, delay);
          };
          input = null;
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
          var errorBox = document.getElementById('error');
          errorBox.style.display = 'inline-block';
          setTimeout(function () {
              errorBox.style.display = 'none';
          }, 3000);
          return false;
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

      document.addEventListener("DOMContentLoaded", function() {
          inputCallback(document.getElementById('ins-input'),getInstances,500);
      });

  </script>

  <div class="container">

      <div class="row">
          <div class="col-md-6 col-md-offset-3 text-center">
              <img src="css/logoBM.png" style="width: 200px; height: 99px">
              <% String lang = request.getParameter("lang");
              if(lang==null) lang = "en"; %>
              <br>
              <% if(lang.equals("es")) { %>
              <h1>Navegación por facetas</h1>
              <% } else { %>
              <h1>Faceted Browsing</h1>
              <% } %>
          </div>
          <div class="col-md-3 text-right">
              <div class="dropdown-right">
                  <button aria-expanded="false" data-toggle="dropdown" type="button" class="btn btn-default dropdown-toggle">
                      <span class="glyphicon glyphicon-globe"></span> Language <span class="caret"></span>
                  </button>
                  <ul class="dropdown-menu dropdown-menu-right">
                      <li><a href="#" onclick="changeLanguage('en')">English</a></li>
                      <li><a href="#" onclick="changeLanguage('es')">Español</a></li>
                  </ul>
              </div>
          </div>
      </div>

      <br><br>

      <div class="row">
      <div class="col-md-6 col-md-offset-3">
          <ul class="nav nav-tabs">
              <li class="active"><a data-toggle="tab" href="#search1">
                  <% if(lang.equals("es")) { %>
                  Tipo
                  <% } else { %>
                  Type
                  <% } %>
              </a></li>
              <li><a data-toggle="tab" href="#search2">
                  <% if(lang.equals("es")) { %>
                  Nombre
                  <% } else { %>
                  Name
                  <% } %>
              </a></li>
          </ul>

          <div class="tab-content">
              <div id="search1" class="tab-pane fade in active">
                  <% if(lang.equals("es")) { %>
                  <h3>Búsqueda por tipo</h3>
                  <% } else { %>
                  <h3>Search by type</h3>
                  <% } %>
                  <br>
                  <div class="alert alert-danger" id="error" style="display: none">
                    <strong>
                        <% if(lang.equals("es")) { %>
                        Error.
                        <% } else { %>
                        Error.
                        <% } %>
                    </strong>
                      <% if(lang.equals("es")) { %>
                      Elige un tipo de la lista.
                      <% } else { %>
                      Choose a type from the list.
                      <% } %>
                  </div>
                  <br>
                  <form action="search" method="get" autocomplete="off" onsubmit="return getQ()">
                      <% if(request.getParameter("lang")!=null && !request.getParameter("lang").isEmpty()) {%>
                      <input readonly type="hidden" id="lang" name="lang" value="<%= request.getParameter("lang").trim() %>">
                      <% } %>

                      <datalist id="instances"></datalist>

                      <div class="form-group">
                          <% if(lang.equals("es")) { %>
                          <label for="ins-input">Elige un tipo:
                                  <% } else { %>
                              <label for="ins-input">Select a type:
                                  <% } %>
                                  <i id="load-icon" style="display: none" class='fa fa-circle-o-notch fa-spin'></i> </label>
                              <input id="ins-input" class="form-control" list="instances">
                              <input type="hidden" name="instance" id="instance-hidden">
                      </div>

                      <% if(lang.equals("es")) { %>
                      <input type="submit" value="Buscar" class="btn btn-default">
                      <% } else { %>
                      <input type="submit" value="Search" class="btn btn-default">
                      <% } %>
                  </form>

              </div>

              <div id="search2" class="tab-pane fade">
                  <% if(lang.equals("es")) { %>
                  <h3>Búsqueda por nombre</h3>
                  <% } else { %>
                  <h3>Search by name</h3>
                  <% } %>
                  <br><br>
                  <form action="search" method="get" autocomplete="off">
                      <% if(request.getParameter("lang")!=null && !request.getParameter("lang").isEmpty()) {%>
                      <input readonly type="hidden" id="lang" name="lang" value="<%= request.getParameter("lang").trim() %>">
                      <% } %>

                      <div class="form-group">
                          <% if(lang.equals("es")) { %>
                          <label for="keyword">Palabra clave:</label>
                          <% } else { %>
                          <label for="keyword">Keyword:</label>
                          <% } %>
                          <input id="keyword" class="form-control" type="text" name="keyword">
                      </div>

                      <% if(lang.equals("es")) { %>
                      <input type="submit" value="Buscar" class="btn btn-default">
                      <% } else { %>
                      <input type="submit" value="Search" class="btn btn-default">
                      <% } %>
                  </form>

              </div>

          </div>

      </div>
      </div>
  </div>

  <footer class="footer">
      <div class="container">
          <div class="text-muted text-right">
              <a href="about.html">About</a>
          </div>
      </div>
  </footer>
  </body>
</html>
