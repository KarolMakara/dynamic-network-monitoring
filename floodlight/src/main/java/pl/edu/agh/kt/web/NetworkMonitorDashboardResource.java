package pl.edu.agh.kt.web;

import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.data.MediaType;

public class NetworkMonitorDashboardResource extends ServerResource {
    
    @Get("html")
    public Representation retrieve() {
        String html = "<!DOCTYPE html>\n" +
"<html lang=\"en\">\n" +
"<head>\n" +
"    <meta charset=\"UTF-8\">\n" +
"    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
"    <title>Network Monitor Dashboard</title>\n" +
"    <script src=\"https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js\"></script>\n" +
"    <style>\n" +
"        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
"        body {\n" +
"            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
"            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);\n" +
"            min-height: 100vh;\n" +
"            padding: 20px;\n" +
"        }\n" +
"        .container {\n" +
"            max-width: 1400px;\n" +
"            margin: 0 auto;\n" +
"        }\n" +
"        h1 {\n" +
"            color: #e0e0e0;\n" +
"            text-align: center;\n" +
"            font-size: 2.5em;\n" +
"            margin-bottom: 30px;\n" +
"            text-shadow: 2px 2px 8px rgba(0,0,0,0.5);\n" +
"        }\n" +
"        .dashboard-grid {\n" +
"            display: grid;\n" +
"            grid-template-columns: 1fr 1fr;\n" +
"            gap: 25px;\n" +
"            margin-bottom: 20px;\n" +
"        }\n" +
"        .card {\n" +
"            background: #242938;\n" +
"            border-radius: 15px;\n" +
"            padding: 25px;\n" +
"            box-shadow: 0 10px 30px rgba(0,0,0,0.4);\n" +
"            border: 1px solid rgba(255,255,255,0.1);\n" +
"        }\n" +
"        .chart-card {\n" +
"            grid-column: 1 / -1; /* Zajmuje całą szerokość góry */\n" +
"            height: 600px;       /* Zwiększona wysokość wykresu */\n" +
"            display: flex;\n" +
"            flex-direction: column;\n" +
"        }\n" +
"        .chart-container {\n" +
"            flex-grow: 1;\n" +
"            position: relative;\n" +
"            min-height: 0;\n" +
"        }\n" +
"        .state-badge {\n" +
"            display: inline-block;\n" +
"            padding: 15px 30px;\n" +
"            border-radius: 50px;\n" +
"            font-size: 1.5em;\n" +
"            font-weight: bold;\n" +
"            color: white;\n" +
"            text-align: center;\n" +
"            transition: all 0.3s ease;\n" +
"            box-shadow: 0 4px 15px rgba(0,0,0,0.3);\n" +
"        }\n" +
"        .state-normal { background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%); }\n" +
"        .state-critical { background: linear-gradient(135deg, #eb3349 0%, #f45c43 100%); }\n" +
"        .metric-row {\n" +
"            display: flex;\n" +
"            justify-content: space-between;\n" +
"            padding: 12px 0;\n" +
"            border-bottom: 1px solid rgba(255,255,255,0.1);\n" +
"        }\n" +
"        .metric-row:last-child { border-bottom: none; }\n" +
"        .metric-label {\n" +
"            color: #a0a0a0;\n" +
"            font-weight: 600;\n" +
"        }\n" +
"        .metric-value {\n" +
"            color: #e0e0e0;\n" +
"            font-weight: bold;\n" +
"        }\n" +
"        .card-title {\n" +
"            font-size: 1.3em;\n" +
"            margin-bottom: 20px;\n" +
"            color: #e0e0e0;\n" +
"            border-bottom: 3px solid #667eea;\n" +
"            padding-bottom: 10px;\n" +
"        }\n" +
"        .status-indicator {\n" +
"            text-align: center;\n" +
"        }\n" +
"        .last-update {\n" +
"            text-align: center;\n" +
"            color: #808080;\n" +
"            font-size: 0.9em;\n" +
"            margin-top: 30px;\n" +
"        }\n" +
"        @media (max-width: 768px) {\n" +
"            .dashboard-grid { grid-template-columns: 1fr; }\n" +
"            .chart-card { height: 400px; }\n" +
"        }\n" +
"    </style>\n" +
"</head>\n" +
"<body>\n" +
"    <div class=\"container\">\n" +
"        <h1>Network Monitor Dashboard</h1>\n" +
"        \n" +
"        <div class=\"dashboard-grid\">\n" +
"            \n" +
"            <div class=\"card chart-card\">\n" +
"                <h2 class=\"card-title\">Real-time Bandwidth</h2>\n" +
"                <div class=\"chart-container\">\n" +
"                    <canvas id=\"bandwidthChart\"></canvas>\n" +
"                </div>\n" +
"            </div>\n" +
"            \n" +
"            \n" +
"            <div class=\"card status-indicator\">\n" +
"                <h2 class=\"card-title\">Network State</h2>\n" +
"                <div class=\"state-badge state-normal\" id=\"stateBadge\">LOADING...</div>\n" +
"                <div style=\"margin-top: 25px;\">\n" +
"                    <div class=\"metric-row\">\n" +
"                        <span class=\"metric-label\">Current Bandwidth:</span>\n" +
"                        <span class=\"metric-value\" id=\"currentBandwidth\">--</span>\n" +
"                    </div>\n" +
"                </div>\n" +
"            </div>\n" +
"            \n" +
"            <div class=\"card\">\n" +
"                <h2 class=\"card-title\">Configuration</h2>\n" +
"                <div class=\"metric-row\">\n" +
"                    <span class=\"metric-label\">Polling Interval:</span>\n" +
"                    <span class=\"metric-value\" id=\"pollingInterval\">--</span>\n" +
"                </div>\n" +
"                <div class=\"metric-row\">\n" +
"                    <span class=\"metric-label\">Low Threshold:</span>\n" +
"                    <span class=\"metric-value\" id=\"lowThreshold\">--</span>\n" +
"                </div>\n" +
"                <div class=\"metric-row\">\n" +
"                    <span class=\"metric-label\">High Threshold:</span>\n" +
"                    <span class=\"metric-value\" id=\"highThreshold\">--</span>\n" +
"                </div>\n" +
"                <div class=\"metric-row\">\n" +
"                    <span class=\"metric-label\">Analysis Window:</span>\n" +
"                    <span class=\"metric-value\" id=\"analysisWindow\">--</span>\n" +
"                </div>\n" +
"            </div>\n" +
"        </div>\n" +
"        \n" +
"        <div class=\"last-update\" id=\"lastUpdate\">Last updated: --</div>\n" +
"    </div>\n" +
"\n" +
"    <script>\n" +
"        const ctx = document.getElementById('bandwidthChart').getContext('2d');\n" +
"        const maxDataPoints = 60;\n" +
"        let timeLabels = [];\n" +
"        let bandwidthData = [];\n" +
"        let currentIntervalId = null;\n" +
"        let currentRefreshInterval = 5000;\n" +
"        \n" +
"        const chart = new Chart(ctx, {\n" +
"            type: 'line',\n" +
"            data: {\n" +
"                labels: timeLabels,\n" +
"                datasets: [{\n" +
"                    label: 'Bandwidth (Mb/s)',\n" +
"                    data: bandwidthData,\n" +
"                    borderColor: 'rgb(139, 157, 255)',\n" +
"                    backgroundColor: 'rgba(139, 157, 255, 0.2)',\n" +
"                    borderWidth: 3,\n" +
"                    tension: 0.4,\n" +
"                    fill: true,\n" +
"                    pointRadius: 3,\n" +
"                    pointHoverRadius: 6,\n" +
"                    pointBackgroundColor: 'rgb(139, 157, 255)',\n" +
"                    pointBorderColor: '#fff',\n" +
"                    pointBorderWidth: 2\n" +
"                }]\n" +
"            },\n" +
"            options: {\n" +
"                responsive: true,\n" +
"                maintainAspectRatio: false,\n" +
"                scales: {\n" +
"                    y: {\n" +
"                        beginAtZero: true,\n" +
"                        title: { display: true, text: 'Bandwidth (Mb/s)', color: '#e0e0e0', font: { size: 14, weight: 'bold' } },\n" +
"                        grid: { color: 'rgba(255, 255, 255, 0.1)' },\n" +
"                        ticks: { color: '#a0a0a0', font: { size: 12 } }\n" +
"                    },\n" +
"                    x: {\n" +
"                        title: { display: true, text: 'Time', color: '#e0e0e0', font: { size: 14, weight: 'bold' } },\n" +
"                        grid: { color: 'rgba(255, 255, 255, 0.1)' },\n" +
"                        ticks: {\n" +
"                            color: '#a0a0a0',\n" +
"                            font: { size: 11 },\n" +
"                            maxRotation: 0,\n" +
"                            minRotation: 0,\n" +
"                            autoSkip: true,\n" +
"                            maxTicksLimit: 15\n" +
"                        }\n" +
"                    }\n" +
"                },\n" +
"                plugins: {\n" +
"                    legend: { display: true, position: 'top', labels: { color: '#e0e0e0' } }\n" +
"                }\n" +
"            }\n" +
"        });\n" +
"        \n" +
"        function scheduleNextUpdate(intervalMs) {\n" +
"            if (currentIntervalId !== null) {\n" +
"                clearInterval(currentIntervalId);\n" +
"            }\n" +
"            currentRefreshInterval = intervalMs;\n" +
"            currentIntervalId = setInterval(updateDashboard, intervalMs);\n" +
"        }\n" +
"        \n" +
"        async function updateDashboard() {\n" +
"            try {\n" +
"                const response = await fetch('/wm/networkmonitor/metrics/json');\n" +
"                const data = await response.json();\n" +
"                \n" +
"                const now = new Date();\n" +
"                const timeStr = now.getHours().toString().padStart(2, '0') + ':' + \n" +
"                               now.getMinutes().toString().padStart(2, '0') + ':' + \n" +
"                               now.getSeconds().toString().padStart(2, '0');\n" +
"                \n" +
"                timeLabels.push(timeStr);\n" +
"                bandwidthData.push(data.currentBandwidthMbps);\n" +
"                \n" +
"                if (timeLabels.length > maxDataPoints) {\n" +
"                    timeLabels.shift();\n" +
"                    bandwidthData.shift();\n" +
"                }\n" +
"                \n" +
"                chart.update();\n" +
"                \n" +
"                const stateBadge = document.getElementById('stateBadge');\n" +
"                stateBadge.textContent = data.currentState;\n" +
"                stateBadge.className = 'state-badge state-' + data.currentState.toLowerCase();\n" +
"                \n" +
"                document.getElementById('currentBandwidth').textContent = data.currentBandwidthMbps.toFixed(2) + ' Mb/s';\n" +
"                document.getElementById('pollingInterval').textContent = data.pollingIntervalMs + ' ms';\n" +
"                document.getElementById('lowThreshold').textContent = data.thresholds.lowTrafficMbps + ' Mb/s';\n" +
"                document.getElementById('highThreshold').textContent = data.thresholds.highTrafficMbps + ' Mb/s';\n" +
"                document.getElementById('analysisWindow').textContent = (data.analysisWindowMs / 1000) + ' seconds';\n" +
"                \n" +
"                document.getElementById('lastUpdate').textContent = 'Last updated: ' + now.toLocaleString();\n" +
"                \n" +
"                if (data.pollingIntervalMs !== currentRefreshInterval) {\n" +
"                    scheduleNextUpdate(data.pollingIntervalMs);\n" +
"                }\n" +
"            } catch (error) {\n" +
"                console.error('Error fetching data:', error);\n" +
"            }\n" +
"        }\n" +
"        \n" +
"        updateDashboard();\n" +
"        scheduleNextUpdate(5000);\n" +
"    </script>\n" +
"</body>\n" +
"</html>";
        
        return new StringRepresentation(html, MediaType.TEXT_HTML);
    }
}
