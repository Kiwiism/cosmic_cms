param(
    [string]$HostName = "127.0.0.1",
    [int]$Port = 7575,
    [int]$Connections = 500,
    [int]$HoldSeconds = 60,
    [int]$ConnectDelayMs = 5
)

$clients = [System.Collections.Generic.List[System.Net.Sockets.TcpClient]]::new()
$failures = 0

try {
    for ($i = 0; $i -lt $Connections; $i++) {
        try {
            $client = [System.Net.Sockets.TcpClient]::new()
            $client.Connect($HostName, $Port)
            $clients.Add($client)
        } catch {
            $failures++
        }

        if ($ConnectDelayMs -gt 0) {
            Start-Sleep -Milliseconds $ConnectDelayMs
        }
    }

    Write-Host "Connected $($clients.Count) sockets; failures: $failures"
    Write-Host "Holding connections for $HoldSeconds seconds. This tests TCP acceptance only."
    Start-Sleep -Seconds $HoldSeconds
} finally {
    foreach ($client in $clients) {
        $client.Dispose()
    }
}
