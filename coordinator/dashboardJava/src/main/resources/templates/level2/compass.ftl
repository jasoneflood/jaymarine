<!-- Styles -->
<style>
#page
{
  margin: auto;
  width: 555px;
  border: 3px solid green;
  padding: 10px;
}
#connections
{
	width: 556px;
	height: 55px;
}
#pilot_connect
{
	width: 50px;
	height: 50px;
	background-image:url("/images/autopilot_icon.fw.png");
	visibility: hidden;
}

#compass_connect
{
	width: 50px;
	height: 50px;
	margin-top: -50px;
	margin-left: 50px;
	background-image:url("/images/compass_icon.fw.png");
	visibility: hidden;
}

#pilot
{
	width: 100px; /* Set the width of the div */
	height: 500px;
  	background-color: yellow; /* Just for visibility */
  	padding: 20px; /* Optional: Add padding for content */
  	text-align:center;
  	border: 3px solid green;
  	padding: 10px;
}

#chartdiv 
{
  width: 400px;
  height: 500px;
  border: 3px solid green;
  padding: 10px;
  margin-left: 130px;
  margin-top: -526px;
}

#azimuth
{
	width: 200px; /* Set the width of the div */
  	margin: 0 auto; /* Center the div horizontally */
  	background-color: lightgray; /* Just for visibility */
  	padding: 20px; /* Optional: Add padding for content */
  	text-align:center;
  	font-size:32px;
  	border: 3px solid green;
  	padding: 10px;
  	margin-top:8px;
}

#target
{
	width: 40px; /* Set the width of the div */
  	margin: 0 auto; /* Center the div horizontally */
  	background-color: lightgray; /* Just for visibility */
  	padding: 20px; /* Optional: Add padding for content */
  	text-align:center;
  	font-size:32px;
}

#subtract
{
	width: 40px; /* Set the width of the div */
  	margin: 0 auto; /* Center the div horizontally */
  	background-color: lightgray; /* Just for visibility */
  	padding: 20px; /* Optional: Add padding for content */
  	text-align:center;
  	font-size:32px;
}

#add
{
	width: 40px; /* Set the width of the div */
  	margin: 0 auto; /* Center the div horizontally */
  	background-color: lightgray; /* Just for visibility */
  	padding: 20px; /* Optional: Add padding for content */
  	text-align:center;
  	font-size:32px;
}

#control
{
	width: 40px; /* Set the width of the div */
  	margin: 0 auto; /* Center the div horizontally */
  	background-color: lightgray; /* Just for visibility */
  	padding: 20px; /* Optional: Add padding for content */
  	text-align:center;
  	font-size:32px;
  	margin-top:10px;
}

#correction
{
	width: 40px; /* Set the width of the div */
  	margin: 0 auto; /* Center the div horizontally */
  	background-color: lightgray; /* Just for visibility */
  	padding: 20px; /* Optional: Add padding for content */
  	text-align:center;
  	font-size:32px;
  	margin-top:10px;
}

#direction
{
	width: 40px; /* Set the width of the div */
  	margin: 0 auto; /* Center the div horizontally */
  	background-color: lightgray; /* Just for visibility */
  	padding: 20px; /* Optional: Add padding for content */
  	text-align:center;
  	font-size:32px;
  	margin-top:10px;
}
#buildDetails
{
	margin-top: -20px;
	height: 20px;
	width: 60px;
	
}

</style>

<!-- Resources -->
<script src="/js/index.js"></script>
<script src="/js/xy.js"></script>
<script src="/js/radar.js"></script>
<script src="/js/Animated.js"></script>
<script src="/js/websocket.js"></script>



<!-- Chart code -->
<script>
<!-- 0=W 90=S 180=E 270=N -->
var newAngle = 0;

am5.ready(function() 
{
	
	// Create root element
	// https://www.amcharts.com/docs/v5/getting-started/#Root_element
	var root = am5.Root.new("chartdiv");
	
	// Set themes
	// https://www.amcharts.com/docs/v5/concepts/themes/
	root.setThemes([
	  am5themes_Animated.new(root)
	]);

	// Create chart
	// https://www.amcharts.com/docs/v5/charts/radar-chart/
	var chart = root.container.children.push(
	  am5radar.RadarChart.new(root, {
	    panX: false,
	    panY: false,
	    startAngle: -90,
	    endAngle: 270
	  })
	);

	// Create axis and its renderer
	// https://www.amcharts.com/docs/v5/charts/radar-chart/gauge-charts/#Axes
	var axisRenderer = am5radar.AxisRendererCircular.new(root, {
	  strokeOpacity: 1,
	  strokeWidth: 5,
	  minGridDistance: 10
	});
	axisRenderer.ticks.template.setAll({
	  forceHidden: true
	});
	axisRenderer.grid.template.setAll({
	  forceHidden: true
	});

	axisRenderer.labels.template.setAll({ forceHidden: true });
	
	var xAxis = chart.xAxes.push(
	  am5xy.ValueAxis.new(root, {
	    maxDeviation: 0,
	    min: 0,
	    max: 360,
	    strictMinMax: true,
	    renderer: axisRenderer
	  })
	);

	// Add clock hand
	// https://www.amcharts.com/docs/v5/charts/radar-chart/gauge-charts/#Clock_hands
	// north
	var axisDataItemN = xAxis.makeDataItem({ value: 0 });
	
	var clockHandN = am5radar.ClockHand.new(root, {
	  pinRadius: 0,
	  radius: am5.percent(90),
	  bottomWidth: 40
	});

	clockHandN.hand.set("fill", am5.color(0xff0000));
	// do not change angle at all
	clockHandN.adapters.add("rotation", function () {
	  return -90;
	});

	axisDataItemN.set(
	  "bullet",
	  am5xy.AxisBullet.new(root, {
	    sprite: clockHandN
	  })
	);

	xAxis.createAxisRange(axisDataItemN);

	//south
	var axisDataItemS = xAxis.makeDataItem({ value: 180 });
	
	var clockHandS = am5radar.ClockHand.new(root, {
	  pinRadius: 0,
	  radius: am5.percent(90),
	  bottomWidth: 40
	});

	// do not change angle at all
	clockHandS.adapters.add("rotation", function () {
	  return 90;
	});

	axisDataItemS.set(
	  "bullet",
	  am5xy.AxisBullet.new(root, {
	    sprite: clockHandS
	  })
	);

	xAxis.createAxisRange(axisDataItemS);

	function createLabel(text, value, tickOpacity) {
  	var axisDataItem = xAxis.makeDataItem({ value: value });
 	 xAxis.createAxisRange(axisDataItem);
 	 var label = axisDataItem.get("label");
 	 label.setAll({
    text: text,
    forceHidden: false,
    inside: true,
    radius: 20
  });

  var tick = axisDataItem
    .get("tick")
    .setAll({
      forceHidden: false,
      strokeOpacity: tickOpacity,
      length: 12 * tickOpacity,
      visible: true,
      inside: true
    });
}

createLabel("N", 0, 1);
createLabel("NE", 45, 1);
createLabel("E", 90, 1);
createLabel("SE", 135, 1);
createLabel("S", 180, 1);
createLabel("SW", 225, 1);
createLabel("W", 270, 1);
createLabel("NW", 315, 1);

for (var i = 0; i < 360; i = i + 5) {
  createLabel("", i, 0.5);
}

setInterval(function () {
  
  chart.animate({
    key: "startAngle",
    to: newAngle,
    duration: 250,
    easing: am5.ease.out(am5.ease.cubic)
  });
  chart.animate({
    key: "endAngle",
    to: newAngle + 360,
    duration: 250,
    easing: am5.ease.out(am5.ease.cubic)
  });
  axisDataItemN.animate({
    key: "value",
    to: am5.math.normalizeAngle(-90 - newAngle),
    duration: 250,
    easing: am5.ease.out(am5.ease.cubic)
  });
  axisDataItemS.animate({
    key: "value",
    to: am5.math.normalizeAngle(90 - newAngle),
    duration: 250,
    easing: am5.ease.out(am5.ease.cubic)
  });
}, 500);

// Make stuff animate on load
chart.appear(1000, 100);

}); // end am5.ready()


</script>

<!-- HTML -->
<div id="page">
<div id="connections">
	<div id="pilot_connect"></div>
	<div id="compass_connect"></div>

</div>
	<div id="pilot">
		Auto Pilot
		<div id="add" onclick="alterHeading(1)">+</div><div id="target">0</div><div id="subtract" onclick="alterHeading(-1)">-</div>
		
		<div id="control" onclick="control()">set</div>
		
		<div id="correction">0</div>
		<div id="direction">-</div>
	</div>
	<div id="chartdiv"></div>
	<div id="azimuth">0</div>
	<div id="buildDetails">${buildversion}</div>
</div>

<script>
function alterHeading(adjustment)
{
	console.log("adjustment:" + adjustment);
	var target = document.getElementById('target');
	var target_value  = target.textContent || target.innerText;
	console.log("target_value:" + target_value);
	var hold = +target_value + (adjustment);
	
	const url = 'http://127.0.0.1:3200/input/autopilotControl';
	const data = {source:'autopilotControl',payload:{status:'on', target: hold}, version:'1'};

	postData(url, data)
    .then(responseData => {
        console.log('Response:', responseData);
    })
    .catch(error => {
        console.error('Error:', error);
    });
	
}
/**********************************/
async function postData(url = '', data = {}) 
{
    const response = await fetch(url, 
    {
        method: 'POST',
        headers: 
        {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    });

    if (!response.ok) 
    {
        throw new Error("HTTP error! status:" + response.status);
    }
    return response.json(); // parses JSON response into native JavaScript objects
}
/**********************************/
function control()
{
	console.log("SET pressed");
	var azimuth = document.getElementById('azimuth');
	var azimuth_value  = azimuth.textContent || azimuth.innerText;
	
	var control = document.getElementById('control');
	var control_value  = control.textContent || control.innerText;
	
	var control_toggle = "off";
	if(control_value.localeCompare("off")==0 || control_value.localeCompare("set")==0)
    {
    	control_toggle = "on";
    }
    if(control_value.localeCompare("on")==0)
    {
    	control_toggle = "off";
    }
	
	//document.getElementById("target").innerHTML = azimuth_value;
	
	const url = 'http://127.0.0.1:3200/input/autopilotControl';
	const data = {source:'autopilotControl',payload:{status:control_toggle, target: azimuth_value}, version:'1'};

	postData(url, data)
    .then(responseData => {
        console.log('Response:', responseData);
    })
    .catch(error => {
        console.error('Error:', error);
    });
}
/**********************************/
function showConnection(connection) 
{
	document.getElementById(connection).style.visibility = 'visible';
  	setTimeout(
    function() {
      document.getElementById(connection).style.visibility = 'hidden';
    }, 1000);
}


function processUpdateFromSocket(data)
{
	console.log("Processing: "  + data);
	var jsonArray = JSON.parse(data);
	
	jsonArray.forEach((item, index) => 
	{
    	console.log("Object at index " + index);
    	console.log("workerName: " + item.workerName);
    	console.log("version: "+ item.version);
    	console.log("data: " + item.data);
    	console.log("data: " + item.worker_ip);
    	
    	var hold = "compass";
    	if(hold.localeCompare(item.workerName)==0)
    	{
    		console.log("have found " + hold + " data");
    		newAngle = +item.data;
    		document.getElementById("azimuth").innerHTML = item.data;
    		showConnection("compass_connect");
    	}
    	console.log('---');
    	
        hold = "autopilotControl_target";
    	if(hold.localeCompare(item.workerName)==0)
    	{
    		console.log("have found " + hold + " data");
    		document.getElementById("target").innerHTML = item.data;
    	}
    	console.log('---');
    	
    	hold = "autopilotControl_status";
    	if(hold.localeCompare(item.workerName)==0)
    	{
    		console.log("have found " + hold + " data");
    		document.getElementById("control").innerHTML = item.data;
    	}
    	console.log('---');
    	
    	hold = "autopilotControl_correction";
    	if(hold.localeCompare(item.workerName)==0)
    	{
    		console.log("have found " + hold + " data");
    		document.getElementById("correction").innerHTML = item.data;
    	}
    	console.log('---');
    	
    	hold = "autopilotControl_turn";
    	if(hold.localeCompare(item.workerName)==0)
    	{
    		console.log("have found " + hold + " data");
    		document.getElementById("direction").innerHTML = item.data;
    	}
    	console.log('---');
    	
    	hold = "autopilotControl_agent";
    	if(hold.localeCompare(item.workerName)==0)
    	{
    		console.log("have found " + hold + " data");
    		showConnection("pilot_connect");
    	}
    	console.log('---');
    	
    	
	});
	
	
	//console.log("data:" + data);
	//var jsonObject = JSON.parse(data);
	//console.log("workerName:", jsonObject.workerName);
	//console.log("version:", jsonObject.version);
	//console.log("data:", jsonObject.data);
	//console.log("worker_ip:", jsonObject.worker_ip);
	
	//newAngle = +jsonObject.data;
	//newAngle = 92;
	//document.getElementById("azimuth").innerHTML = jsonObject.data;
}
</script>
