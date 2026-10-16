#requires -Version 7.0
# LOCAL DEMO ONLY — verification hors ligne, sans Pester, serveur ni PostgreSQL.
# Invoke-RestMethod est remplace UNIQUEMENT dans la portee de ce script de verification.
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
class ReportDemoHttpException : System.Exception {
    [object]$Response
    ReportDemoHttpException([int]$status) : base('Reponse simulee hors ligne') {
        $this.Response = [pscustomobject]@{ StatusCode = $status }
    }
}
function Assert-DemoCheck([bool]$Condition, [string]$Message) {
    if (!$Condition) { throw "ECHEC : $Message" }
}
function Reset-FakeApi {
    $script:fake = @{
        users = [Collections.Generic.List[object]]::new()
        clients = [Collections.Generic.List[object]]::new()
        demandes = [Collections.Generic.List[object]]::new()
        writes = 0; calls = 0; offline = $false; interrupt = $false
    }
    $fake.users.Add([pscustomobject]@{ id = 1; nom = 'Administration locale'; email = 'admin@example.test'; actif = $true; roles = @('ADMINISTRATEUR') })
}
function Invoke-RestMethod {
    param($Uri, $Method, $TimeoutSec, $MaximumRedirection, $ErrorAction, $Headers = @{}, $ContentType, $Body)
    $fake.calls++
    if ($fake.offline) { throw [ReportDemoHttpException]::new(503) }
    $url = [uri]$Uri
    $path = $url.AbsolutePath.Replace('/api/v1', '')
    $query = [System.Web.HttpUtility]::ParseQueryString($url.Query)
    $payload = if ($null -ne $Body) { [Text.Encoding]::UTF8.GetString($Body) | ConvertFrom-Json } else { $null }
    if ($path -eq '/auth/login') {
        $user = @($fake.users | Where-Object email -eq $payload.email)[0]
        return [pscustomobject]@{ accessToken = "token-$($user.id)"; user = $user }
    }
    if (!$Headers.ContainsKey('Authorization')) { throw [ReportDemoHttpException]::new(401) }
    $id = [int]($Headers.Authorization -replace 'Bearer token-', '')
    $actor = @($fake.users | Where-Object id -eq $id)[0]
    $isAdmin = 'ADMINISTRATEUR' -in $actor.roles
    $isRt = 'RESPONSABLE_TECHNIQUE' -in $actor.roles
    if ($Method -eq 'GET' -and $path -in @('/utilisateurs', '/clients', '/demandes')) {
        if ($path -eq '/utilisateurs') {
            Assert-DemoCheck $isAdmin 'lecture utilisateurs par ADM'
            $items = @($fake.users.ToArray())
        } elseif ($path -eq '/clients') {
            Assert-DemoCheck $isRt 'lecture clients par RT'
            $items = @($fake.clients.ToArray() | Where-Object email -eq $query['recherche'])
        } else {
            Assert-DemoCheck $isRt 'lecture demandes par RT'
            $items = @($fake.demandes.ToArray() | Where-Object titre -eq $query['recherche'])
        }
        # Petites pages pour exercer la boucle de pagination sans reproduire le serveur.
        $page = [int]$query['page']
        return [pscustomobject]@{ items = @($items | Select-Object -Skip ($page * 2) -First 2); page = $page; totalPages = [math]::Ceiling($items.Count / 2) }
    }
    if ($path -eq '/utilisateurs' -and $Method -eq 'POST') {
        Assert-DemoCheck $isAdmin 'creation utilisateur par ADM'
        Assert-DemoCheck ('ADMINISTRATEUR' -notin $payload.rolesMetier) 'aucune attribution ADM'
        $user = [pscustomobject]@{ id = $fake.users.Count + 1; nom = $payload.nom; email = $payload.email; actif = $payload.actif; roles = @($payload.rolesMetier) }
        $fake.users.Add($user); $fake.writes++
        return $user
    }
    if ($path -eq '/demandes' -and $Method -eq 'POST') {
        Assert-DemoCheck $isRt 'creation demande par RT'
        Assert-DemoCheck (!$payload.PSObject.Properties['statut']) 'aucun statut impose a la creation'
        if ($payload.PSObject.Properties['nouveauClient']) {
            Assert-DemoCheck (!$payload.PSObject.Properties['clientId']) 'choix client exclusif'
            $client = $payload.nouveauClient
            $client | Add-Member id ($fake.clients.Count + 1)
            $fake.clients.Add($client)
        } else { $client = @($fake.clients | Where-Object id -eq $payload.clientId)[0] }
        $detail = [pscustomobject]@{
            reference = "DT-$($fake.demandes.Count + 1)"; titre = $payload.titre; description = $payload.description
            categorie = $payload.categorie; priorite = $payload.priorite; statut = 'NOUVELLE'; client = $client
            createur = $actor; agentAffecte = $null; descriptionTraitement = $null; solution = $null; motifAnnulation = $null
        }
        $fake.demandes.Add($detail); $fake.writes++
        if ($fake.interrupt) { $fake.interrupt = $false; throw [ReportDemoHttpException]::new(503) }
        return $detail
    }
    if ($path -match '^/demandes/(DT-\d+)(?:/(.+))?$') {
        $reference = $Matches[1]
        $action = if ($Matches.ContainsKey(2)) { $Matches[2] } else { '' }
        $detail = @($fake.demandes | Where-Object reference -eq $reference)[0]
        if ($Method -eq 'GET' -and !$action) { return $detail }
        if ($action -in @('affectation', 'cloture', 'annulation')) { Assert-DemoCheck $isRt 'action responsable par RT' }
        else { Assert-DemoCheck ($null -ne $detail.agentAffecte -and $actor.id -eq $detail.agentAffecte.id -and 'AGENT_TECHNIQUE' -in $actor.roles) 'action AT par agent affecte' }
        switch ($action) {
            'affectation' { Assert-DemoCheck ($Method -eq 'PUT' -and $detail.statut -eq 'NOUVELLE') 'affectation valide'; $detail.agentAffecte = @($fake.users | Where-Object id -eq $payload.agentId)[0]; $detail.statut = 'ASSIGNEE' }
            'demarrage-traitement' { Assert-DemoCheck ($Method -eq 'POST' -and $detail.statut -eq 'ASSIGNEE') 'demarrage valide'; $detail.statut = 'EN_COURS' }
            'traitement' {
                Assert-DemoCheck ($Method -eq 'PATCH' -and $detail.statut -eq 'EN_COURS') 'traitement valide'
                if ($payload.PSObject.Properties['descriptionTraitement']) { $detail.descriptionTraitement = $payload.descriptionTraitement }
                if ($payload.PSObject.Properties['solution']) { $detail.solution = $payload.solution }
            }
            'resolution' { Assert-DemoCheck ($Method -eq 'POST' -and $detail.statut -eq 'EN_COURS' -and !!$detail.solution) 'resolution avec solution'; $detail.statut = 'RESOLUE' }
            'cloture' { Assert-DemoCheck ($Method -eq 'POST' -and $detail.statut -eq 'RESOLUE') 'cloture valide'; $detail.statut = 'CLOTUREE' }
            'annulation' { Assert-DemoCheck ($Method -eq 'POST' -and $detail.statut -eq 'NOUVELLE') 'annulation valide'; $detail.statut = 'ANNULEE'; $detail.motifAnnulation = $payload.motif }
            default { throw "Route non prevue : $Method $path" }
        }
        $fake.writes++
        return $detail
    }
    throw "Route non prevue : $Method $path"
}
$prepare = Join-Path $PSScriptRoot 'prepare-report-data.ps1'
# Valeur fictive exclusivement pour le transport simule, jamais envoyee au reseau.
$demoSecret = ConvertTo-SecureString 'LOCAL-DEMO-ONLY-OFFLINE' -AsPlainText -Force
$credential = [pscredential]::new('admin@example.test', $demoSecret)
function Run-Preparation { & $prepare -AdminCredential $credential -DemoPassword $demoSecret }
Reset-FakeApi
$null = & $prepare -Plan
Assert-DemoCheck ($fake.calls -eq 0) 'Plan sans reseau'
$result = @(Run-Preparation)
Assert-DemoCheck ($result.Count -eq 9 -and $fake.clients.Count -eq 4 -and $fake.users.Count -eq 4) '9 demandes, 4 clients, 3 comptes ajoutes'
foreach ($status in @('NOUVELLE', 'ASSIGNEE', 'EN_COURS', 'RESOLUE', 'CLOTUREE', 'ANNULEE')) {
    $expectedCount = if ($status -in @('NOUVELLE', 'ASSIGNEE', 'EN_COURS')) { 2 } else { 1 }
    Assert-DemoCheck (@($result | Where-Object Statut -eq $status).Count -eq $expectedCount) "repartition $status"
}
$writes = $fake.writes
$null = Run-Preparation
Assert-DemoCheck ($fake.writes -eq $writes) 'relance sans aucune ecriture'
Reset-FakeApi
$fake.interrupt = $true
$failed = $false
try { $null = Run-Preparation } catch { $failed = $true }
Assert-DemoCheck ($failed -and $fake.demandes.Count -eq 1) 'interruption apres creation persistante'
$null = Run-Preparation
Assert-DemoCheck ($fake.demandes.Count -eq 9 -and $fake.clients.Count -eq 4) 'reprise sans doublon apres reponse perdue'
$fake.demandes[0].description = 'Contenu modifie manuellement'
$writes = $fake.writes; $failed = $false
try { $null = Run-Preparation } catch { $failed = $true }
Assert-DemoCheck ($failed -and $writes -eq $fake.writes) 'collision sans ecrasement'
Reset-FakeApi
$fake.offline = $true; $failed = $false
try { $null = Run-Preparation } catch { $failed = $true }
Assert-DemoCheck ($failed -and $fake.writes -eq 0) 'backend absent sans ecriture'
$failed = $false
try { $null = & $prepare -Plan -BaseUrl 'https://example.com/api/v1' } catch { $failed = $true }
Assert-DemoCheck $failed 'destination distante refusee'
Write-Host 'OK : verification hors ligne (plan, roles/routes, pagination, repartition, relance, reprise, collision, indisponibilite, URL locale).'
Write-Host 'Ces controles ne prouvent pas une execution sur Spring Boot/PostgreSQL.'
