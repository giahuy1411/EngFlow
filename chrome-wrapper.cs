using System;
using System.IO;
using System.Net;
using System.Text.RegularExpressions;
using System.Threading;

class Program {
    static void Main(string[] args) {
        // Log arguments to project folder if possible
        try {
            File.AppendAllText(@"C:\Users\ASUS\Documents\LAPTRINH\engflow\arguments.log", "Args: " + string.Join(" ", args) + "\n");
        } catch {}

        // Resolve local LAN IP address to bypass AppContainer loopback restriction
        string localIp = "127.0.0.1";
        try {
            foreach (var ip in Dns.GetHostEntry(Dns.GetHostName()).AddressList) {
                if (ip.AddressFamily == System.Net.Sockets.AddressFamily.InterNetwork && !ip.ToString().StartsWith("127.")) {
                    localIp = ip.ToString();
                    break;
                }
            }
        } catch {}

        try {
            File.AppendAllText(@"C:\Users\ASUS\Documents\LAPTRINH\engflow\arguments.log", "Resolved local LAN IP: " + localIp + "\n");
        } catch {}

        string wsUrl = null;
        for (int i = 0; i < 30; i++) {
            try {
                // Connect via local LAN IP
                HttpWebRequest request = (HttpWebRequest)WebRequest.Create("http://" + localIp + ":9222/json/version");
                request.Timeout = 1000;
                using (HttpWebResponse response = (HttpWebResponse)request.GetResponse())
                using (StreamReader reader = new StreamReader(response.GetResponseStream())) {
                    string html = reader.ReadToEnd();
                    Match match = Regex.Match(html, "\"webSocketDebuggerUrl\":\\s*\"([^\"]+)\"");
                    if (match.Success) {
                        wsUrl = match.Groups[1].Value;
                        // Replace 127.0.0.1 with local LAN IP in the WS endpoint
                        wsUrl = wsUrl.Replace("127.0.0.1", localIp);
                        break;
                    }
                }
            } catch (Exception e) {
                try {
                    File.AppendAllText(@"C:\Users\ASUS\Documents\LAPTRINH\engflow\arguments.log", "Retry " + i + " failed: " + e.Message + "\n");
                } catch {}
                Thread.Sleep(500);
            }
        }

        if (wsUrl != null) {
            Console.WriteLine("DevTools listening on " + wsUrl);
            try {
                File.AppendAllText(@"C:\Users\ASUS\Documents\LAPTRINH\engflow\arguments.log", "Proxying to: " + wsUrl + "\n");
            } catch {}
            // Keep running to prevent Puppeteer from thinking the browser crashed
            Thread.Sleep(Timeout.Infinite);
        } else {
            Console.Error.WriteLine("Error: Could not retrieve webSocketDebuggerUrl from Brave browser on port 9222.");
            try {
                File.AppendAllText(@"C:\Users\ASUS\Documents\LAPTRINH\engflow\arguments.log", "Error: Failed to retrieve wsUrl\n");
            } catch {}
            Environment.Exit(1);
        }
    }
}
