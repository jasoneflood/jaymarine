/*************************************/
// Jason Flood
// Mark Deegan
// November 2024
//
// index.html code for md_esp_compass
/*************************************/
const char index_html[] PROGMEM = R"rawliteral(
<!DOCTYPE HTML>
<html>
<head>
  <title>Auto Pilot Manager</title>
  <style>
   
  </style>
  </head>
  <body>
  <div id=pageContainer>
    <div id=pageContent>
        <div id=pageTitle>Auto Pilot Manager
          <div id=version></div>
        </div>
        <div id=pageLinks><a href=\reset>reset</a> | <a href=\about>about</a> </div>
          <div id=settingsDetails>MD: Settings update on change. </div>
          <div id=splitContent>
            <div id=settings>
              <table>
               %ROWPLACEHOLDER_VAR%
              </table>
            </div>
            <div id=headings>
            </div>
            <div id=controls>
              <button type="button" onclick="testScript(1)">test socket</button>
            </div>
    </div>
  </div>

<script>
 %SCRIPT_VAR%
</script>
</body>
</html>
)rawliteral";
/*************************************/