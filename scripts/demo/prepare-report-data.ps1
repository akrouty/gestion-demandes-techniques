#requires -Version 7.0
<#
.SYNOPSIS
LOCAL DEMO ONLY : prepare neuf demandes fictives via les API V1 existantes.
.DESCRIPTION
Backend local demarre sur PostgreSQL reel, administrateur existant requis.
Aucun SQL, aucune IA, aucun changement de configuration, aucun lancement automatique.
Les secrets sont saisis sans echo et restent en memoire. Voir docs/demo/REPORT-CAPTURE-SCENARIO.md.
.EXAMPLE
.\scripts\demo\prepare-report-data.ps1 -Plan
.EXAMPLE
.\scripts\demo\prepare-report-data.ps1
#>
[CmdletBinding()]
param(
    [uri]$BaseUrl = 'http://localhost:8080/api/v1',
    [pscredential]$AdminCredential,
    [securestring]$DemoPassword,
    [switch]$Plan
)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if ($BaseUrl.Scheme -notin @('http', 'https') -or
    $BaseUrl.Host -notin @('localhost', '127.0.0.1', '[::1]', '::1') -or
    $BaseUrl.UserInfo -or $BaseUrl.Query -or $BaseUrl.Fragment -or
    $BaseUrl.AbsolutePath.TrimEnd('/') -ne '/api/v1') {
    throw 'LOCAL DEMO ONLY : URL attendue http(s)://localhost:port/api/v1 (ou adresse de boucle locale).'
}
$apiRoot = $BaseUrl.AbsoluteUri.TrimEnd('/')
$accounts = @(
    @{ nom = 'Nadia Belrive'; email = 'rapport.rt@example.test'; role = 'RESPONSABLE_TECHNIQUE' }
    @{ nom = 'Julien Montel'; email = 'rapport.agent.a@example.test'; role = 'AGENT_TECHNIQUE' }
    @{ nom = 'Lina Valcour'; email = 'rapport.agent.b@example.test'; role = 'AGENT_TECHNIQUE' }
)
$clients = @(
    @{ nom = 'Orvane Industries'; email = 'ingenierie@orvane.example.test'; telephone = '+33 1 99 00 61 01' }
    @{ nom = 'Veloria Conditionnement'; email = 'methodes@veloria.example.test'; telephone = '+33 1 99 00 61 02' }
    @{ nom = 'Nerelis Logistique'; email = 'maintenance@nerelis.example.test'; telephone = '+33 1 99 00 61 03' }
    @{ nom = 'Caldoria Equipements'; email = 'projets@caldoria.example.test'; telephone = '+33 1 99 00 61 04' }
)
# Aucun de ces clients/titres n'est celui du parcours manuel reserve.
$dataset = @(
    @{ titre = 'Revue des dangers du stockage de solvants'; client = 0; categorie = 'ETUDE_DANGERS'; priorite = 'HAUTE'; statut = 'NOUVELLE'; agent = 0
       description = 'Le projet de regroupement des armoires de solvants nécessite une revue des scénarios de fuite et des sources potentielles dʼinflammation. Identifier les données manquantes et préparer le périmètre de lʼétude avant la réunion de conception.'; traitement = ''; solution = '' }
    @{ titre = 'Consolidation du dossier technique de la ligne de conditionnement'; client = 1; categorie = 'DOSSIER_TECHNIQUE'; priorite = 'BASSE'; statut = 'NOUVELLE'; agent = 0
       description = 'Les plans de la ligne et les notices des équipements sont répartis entre plusieurs versions. Préparer un inventaire des pièces et une arborescence documentaire cohérente pour la prochaine revue du dossier.'; traitement = ''; solution = '' }
    @{ titre = 'Vérification des charges sur la plateforme de maintenance'; client = 3; categorie = 'NOTE_CALCUL'; priorite = 'MOYENNE'; statut = 'ASSIGNEE'; agent = 1
       description = 'Une plateforme doit recevoir un nouvel outillage de maintenance. Examiner les hypothèses de charge, les appuis et les documents disponibles afin de préparer la note de calcul avant toute modification.'; traitement = ''; solution = '' }
    @{ titre = 'Revue du plan de protection incendie de lʼentrepôt'; client = 2; categorie = 'PROTECTION_INCENDIE'; priorite = 'HAUTE'; statut = 'ASSIGNEE'; agent = 2
       description = 'La réorganisation des zones de stockage modifie les circulations et les accès aux moyens de première intervention. Comparer le plan actuel avec la nouvelle implantation et relever les points à soumettre à validation.'; traitement = ''; solution = '' }
    @{ titre = 'Analyse des risques du transfert de liquide en atelier'; client = 0; categorie = 'ANALYSE_RISQUES_INDUSTRIELS'; priorite = 'CRITIQUE'; statut = 'EN_COURS'; agent = 1
       description = 'Un écart entre la procédure de transfert et lʼimplantation projetée a été relevé avant mise en service. Examiner les risques de débordement et dʼexposition ; le démarrage reste suspendu dans lʼattente de la revue technique.'
       traitement = 'Les documents du procédé ont été rapprochés du plan dʼimplantation. Les scénarios de débordement et les points de surveillance sont recensés ; les échanges sur les mesures de maîtrise restent en cours.'; solution = '' }
    @{ titre = 'Préparation de la revue documentaire réglementaire'; client = 1; categorie = 'ASSISTANCE_REGLEMENTAIRE'; priorite = 'MOYENNE'; statut = 'EN_COURS'; agent = 2
       description = 'Préparer la revue documentaire liée à lʼévolution de la ligne de conditionnement. Identifier les pièces disponibles, les documents à actualiser et les questions à faire confirmer par les interlocuteurs compétents.'
       traitement = 'Lʼinventaire des pièces a été établi et les versions obsolètes identifiées. Une liste des compléments attendus est en cours de consolidation avec le responsable du dossier.'; solution = '' }
    @{ titre = 'Clarification des interfaces du réseau dʼair comprimé'; client = 3; categorie = 'AUTRE'; priorite = 'MOYENNE'; statut = 'RESOLUE'; agent = 1
       description = 'Les limites de fourniture entre le réseau existant et le nouvel équipement ne sont pas explicites sur les plans. Produire une synthèse des interfaces et des responsabilités documentaires pour la revue du responsable technique.'
       traitement = 'Les plans et les listes de raccordements ont été comparés. Les points dʼinterface et les pièces attendues de chaque intervenant ont été regroupés dans une synthèse.'
       solution = 'La synthèse des interfaces est finalisée et les ambiguïtés documentaires sont levées. Le dossier est prêt pour lʼexamen du responsable technique.' }
    @{ titre = 'Mise en cohérence du dossier de ventilation de lʼatelier'; client = 0; categorie = 'DOSSIER_TECHNIQUE'; priorite = 'BASSE'; statut = 'CLOTUREE'; agent = 2
       description = 'Les repères des équipements de ventilation diffèrent entre le schéma de principe et la nomenclature. Harmoniser les documents et remettre une version cohérente pour archivage après examen.'
       traitement = 'Les repères ont été rapprochés de la nomenclature et les incohérences documentaires corrigées. Une relecture croisée du schéma et de la liste des équipements a été réalisée.'
       solution = 'Les documents présentent désormais des repères concordants et une version commune. Le responsable technique dispose du dossier consolidé pour clôture.' }
    @{ titre = 'Étude de déplacement de la zone de préparation'; client = 2; categorie = 'ANALYSE_RISQUES_INDUSTRIELS'; priorite = 'MOYENNE'; statut = 'ANNULEE'; agent = 0
       description = 'Étudier les incidences dʼun déplacement envisagé de la zone de préparation sur les flux et les circulations internes. Le périmètre devait être confirmé après la revue dʼimplantation.'; traitement = ''; solution = '' }
)
$cancellation = 'Le projet de déplacement a été abandonné lors de la revue dʼimplantation ; le besoin initial ne sʼapplique plus.'
if ($Plan) {
    Write-Host 'LOCAL DEMO ONLY — aperçu sans réseau, sans écriture et sans demande de secret.'
    $accounts | ForEach-Object { [pscustomobject]$_ } | Format-Table nom, email, role
    $dataset | ForEach-Object { [pscustomobject]$_ } | Format-Table titre, statut, categorie, priorite
    return
}
function Invoke-DemoApi {
    param([string]$Method, [string]$Path, [string]$Token = '', [object]$Body = $null)
    $options = @{ Uri = "$apiRoot$Path"; Method = $Method; TimeoutSec = 20; MaximumRedirection = 0; ErrorAction = 'Stop'; Headers = @{} }
    if ($Token) { $options.Headers.Authorization = "Bearer $Token" }
    if ($null -ne $Body) {
        $options.ContentType = 'application/json; charset=utf-8'
        $options.Body = [Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 8 -Compress))
    }
    try { Invoke-RestMethod @options }
    catch {
        $status = 0
        if ($_.Exception.PSObject.Properties['Response'] -and $_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
        }
        $reason = switch ($status) {
            400 { 'Vérifier les champs et la politique locale de mot de passe.' }
            401 { 'Identifiants incorrects, compte inactif ou session expirée. Relancer avec les mêmes comptes.' }
            403 { 'Rôle ou agent affecté incompatible.' }
            409 { 'Conflit métier ; vérifier les données existantes avant de relancer.' }
            default { 'Backend local indisponible ou réponse non conforme. Vérifier son démarrage et PostgreSQL.' }
        }
        # Ni corps de réponse, ni credential, ni JWT dans le diagnostic.
        throw "Échec $Method $Path (HTTP $status). $reason Aucune relance automatique de cette écriture."
    }
}
function Connect-Demo {
    param([pscredential]$Credential, [string]$Role)
    $session = Invoke-DemoApi POST '/auth/login' -Body @{ email = $Credential.UserName; password = $Credential.GetNetworkCredential().Password }
    if (!$session.accessToken -or $Role -notin $session.user.roles) { throw "Le compte fourni doit posséder le rôle $Role." }
    return $session
}
function Get-DemoPages {
    param([string]$Path, [string]$Token)
    $page = 0
    do {
        $separator = if ($Path.Contains('?')) { '&' } else { '?' }
        $response = Invoke-DemoApi GET "$Path${separator}page=$page&size=100" $Token
        if ($response.page -ne $page -or $response.totalPages -lt 0) { throw "Pagination inattendue : $Path." }
        foreach ($item in $response.items) { $item }
        $page++
    } while ($page -lt $response.totalPages)
}
function Assert-DemoUser {
    param($User, $Expected)
    if ($User.nom -cne $Expected.nom -or !$User.actif -or @($User.roles).Count -ne 1 -or $Expected.role -notin $User.roles) {
        throw "Collision ou compte modifié : $($Expected.email). Aucun changement de nom, de rôle, dʼétat ou de mot de passe ne sera forcé."
    }
}
function Find-DemoClient {
    param($Expected, [string]$Token)
    $query = [uri]::EscapeDataString($Expected.email)
    $matches = @(Get-DemoPages "/clients?sort=id,asc&recherche=$query" $Token | Where-Object { $_.email -ieq $Expected.email })
    if ($matches.Count -gt 1) { throw "Plusieurs clients portent lʼemail fictif $($Expected.email). Résoudre cette ambiguïté manuellement." }
    if ($matches.Count -eq 1) {
        if ($matches[0].nom -cne $Expected.nom -or $matches[0].telephone -cne $Expected.telephone) { throw "Collision sur le client $($Expected.nom)." }
        return $matches[0]
    }
    return $null
}
# Empêche deux exécutions simultanées sur cette machine (pas de garantie distribuée).
$mutex = [Threading.Mutex]::new($false, 'Local\GestionDemandesReportDemoV1')
$locked = $false
try {
    try { $locked = $mutex.WaitOne(0) } catch [Threading.AbandonedMutexException] { $locked = $true }
    if (!$locked) { throw 'Une préparation de démonstration est déjà en cours sur cette machine.' }
    Write-Host 'LOCAL DEMO ONLY — base PostgreSQL locale de démonstration attendue. Aucune purge.'
    # Lecture protégée sans JWT : un backend V1 démarré doit répondre 401.
    try {
        $null = Invoke-RestMethod -Uri "$apiRoot/demandes?page=0&size=1" -Method Get -TimeoutSec 10 -MaximumRedirection 0 -ErrorAction Stop
        throw 'La route protégée répond sans authentification : préparation arrêtée.'
    } catch {
        if (!($_.Exception.PSObject.Properties['Response']) -or !$_.Exception.Response -or [int]$_.Exception.Response.StatusCode -ne 401) {
            throw 'Backend V1 local non joignable ou réponse inattendue. Démarrer le backend avec PostgreSQL, puis relancer.'
        }
    }
    if (!$AdminCredential) { $AdminCredential = Get-Credential -Message 'Administrateur LOCAL existant (jamais enregistré par le script)' }
    $admin = Connect-Demo $AdminCredential 'ADMINISTRATEUR'
    if (!$DemoPassword) { $DemoPassword = Read-Host 'LOCAL DEMO ONLY : mot de passe dédié commun aux trois comptes fictifs (identique lors des relances)' -AsSecureString }
    if (!$DemoPassword.Length) { throw 'Un mot de passe de démonstration non vide est requis.' }
    $users = @(Get-DemoPages '/utilisateurs?sort=id,asc' $admin.accessToken)
    # Vérifier les collisions connues avant de créer les comptes manquants.
    foreach ($expected in $accounts) {
        $found = @($users | Where-Object { $_.email -ieq $expected.email })
        if ($found.Count -gt 1) { throw "Email utilisateur ambigu : $($expected.email)." }
        if ($found.Count -eq 1) { Assert-DemoUser $found[0] $expected }
    }
    $sessions = @()
    foreach ($expected in $accounts) {
        $found = @($users | Where-Object { $_.email -ieq $expected.email })
        if (!$found.Count) {
            $body = @{ nom = $expected.nom; email = $expected.email; actif = $true; rolesMetier = @($expected.role); password = ([pscredential]::new($expected.email, $DemoPassword)).GetNetworkCredential().Password }
            $user = Invoke-DemoApi POST '/utilisateurs' $admin.accessToken $body
            Assert-DemoUser $user $expected
            Write-Host "Créé : $($expected.nom) — $($expected.email)"
        } else { Write-Host "Réutilisé : $($expected.nom) — $($expected.email)" }
        $sessions += Connect-Demo ([pscredential]::new($expected.email, $DemoPassword)) $expected.role
    }
    $rt = $sessions[0]
    $summary = @()
    foreach ($entry in $dataset) {
        $expectedClient = $clients[$entry.client]
        $client = Find-DemoClient $expectedClient $rt.accessToken
        $query = [uri]::EscapeDataString($entry.titre)
        $matches = @(Get-DemoPages "/demandes?sort=dateCreation,asc&recherche=$query" $rt.accessToken | Where-Object { $_.titre -ceq $entry.titre })
        if ($matches.Count -gt 1) { throw "Titre ambigu : $($entry.titre). Aucune nouvelle demande créée." }
        if ($matches.Count) {
            $reference = [uri]::EscapeDataString($matches[0].reference)
            $detail = Invoke-DemoApi GET "/demandes/$reference" $rt.accessToken
            if (!$client -or $detail.client.id -ne $client.id -or $detail.createur.id -ne $rt.user.id -or
                $detail.description -cne $entry.description -or $detail.categorie -ne $entry.categorie -or $detail.priorite -ne $entry.priorite) {
                throw "La demande $($detail.reference) ne correspond pas au jeu prévu. Aucun écrasement."
            }
            Write-Host "Reprise : $($detail.reference) — $($entry.titre) [$($detail.statut)]"
        } else {
            $body = @{ titre = $entry.titre; description = $entry.description; categorie = $entry.categorie; priorite = $entry.priorite }
            if ($client) { $body.clientId = $client.id } else { $body.nouveauClient = $expectedClient }
            $detail = Invoke-DemoApi POST '/demandes' $rt.accessToken $body
            Write-Host "Créée : $($detail.reference) — $($entry.titre)"
            if (!$client) { Write-Host "Client créé avec la demande : $($expectedClient.nom)" }
        }
        $reference = [uri]::EscapeDataString($detail.reference)
        $path = "/demandes/$reference"
        $steps = @('NOUVELLE', 'ASSIGNEE', 'EN_COURS', 'RESOLUE', 'CLOTUREE')
        if ($entry.statut -eq 'ANNULEE') { $steps = @('NOUVELLE', 'ANNULEE') }
        $current = [array]::IndexOf($steps, $detail.statut)
        $target = [array]::IndexOf($steps, $entry.statut)
        if ($current -lt 0 -or $current -gt $target) { throw "État divergent pour $($detail.reference) : $($detail.statut). Aucun retour arrière." }
        $agentSession = $sessions[$entry.agent]
        if ($null -ne $detail.agentAffecte -and ($entry.agent -eq 0 -or $detail.agentAffecte.id -ne $agentSession.user.id)) { throw "Agent différent pour $($detail.reference). Aucune réaffectation forcée." }
        foreach ($pair in @(@('descriptionTraitement', 'traitement'), @('solution', 'solution'))) {
            if ($detail.($pair[0]) -and $detail.($pair[0]) -cne $entry[$pair[1]]) { throw "Contenu modifié pour $($detail.reference) : $($pair[0]). Aucun écrasement." }
        }
        if ($entry.statut -eq 'ANNULEE') {
            if ($detail.statut -eq 'NOUVELLE') { $detail = Invoke-DemoApi POST "$path/annulation" $rt.accessToken @{ motif = $cancellation } }
            if ($detail.motifAnnulation -cne $cancellation) { throw "Motif différent pour $($detail.reference)." }
        } else {
            if ($detail.statut -eq 'NOUVELLE' -and $target -ge 1) { $detail = Invoke-DemoApi PUT "$path/affectation" $rt.accessToken @{ agentId = $agentSession.user.id } }
            if ($detail.statut -eq 'ASSIGNEE' -and $target -ge 2) { $detail = Invoke-DemoApi POST "$path/demarrage-traitement" $agentSession.accessToken }
            if ($detail.statut -eq 'EN_COURS') {
                $body = @{}
                if ($entry.traitement -and !$detail.descriptionTraitement) { $body.descriptionTraitement = $entry.traitement }
                if ($entry.solution -and !$detail.solution) { $body.solution = $entry.solution }
                if ($body.Count) { $detail = Invoke-DemoApi PATCH "$path/traitement" $agentSession.accessToken $body }
                if ($target -ge 3) { $detail = Invoke-DemoApi POST "$path/resolution" $agentSession.accessToken }
            }
            if ($detail.statut -eq 'RESOLUE' -and $target -ge 4) { $detail = Invoke-DemoApi POST "$path/cloture" $rt.accessToken }
        }
        $detail = Invoke-DemoApi GET $path $rt.accessToken
        if ($detail.statut -ne $entry.statut) { throw "État final inattendu : $($detail.reference)." }
        if ($entry.agent -gt 0 -and ($null -eq $detail.agentAffecte -or $detail.agentAffecte.id -ne $agentSession.user.id)) { throw "Affectation finale inattendue : $($detail.reference)." }
        if ($entry.traitement -and $detail.descriptionTraitement -cne $entry.traitement) { throw "Traitement final absent ou différent : $($detail.reference)." }
        if ($entry.solution -and $detail.solution -cne $entry.solution) { throw "Solution finale absente ou différente : $($detail.reference)." }
        Write-Host "Prête : $($detail.reference) [$($detail.statut)]"
        $summary += [pscustomobject]@{ Reference = $detail.reference; Titre = $detail.titre; Statut = $detail.statut; Client = $detail.client.nom }
    }
    Write-Host 'Neuf demandes vérifiées par GET. Le client Asterive et le scénario manuel restent réservés.'
    $summary
} finally {
    # Rien n'est enregistré sur disque : JWT et mots de passe restent des variables de cette exécution.
    $AdminCredential = $null
    $DemoPassword = $null
    if ($locked) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
