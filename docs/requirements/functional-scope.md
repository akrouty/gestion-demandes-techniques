# Périmètre fonctionnel V1

## Statut

**V1 fonctionnelle gelée**

Ce document décrit uniquement les décisions fonctionnelles validées pour la V1.
Toute évolution future devra être justifiée par un besoin réel ou par la correction d'une incohérence.

---

# 1. Acteurs

L'application est une application interne à l'entreprise.

Trois rôles applicatifs sont retenus :

- Responsable technique
- Agent technique
- Administrateur

Une même personne peut cumuler plusieurs rôles applicatifs.

Le rôle Administrateur reste distinct des rôles métier.

Le Client n'est pas un utilisateur de l'application.
Il est conservé uniquement comme entité métier associée aux demandes.

---

# 2. Responsabilités

## 2.1 Responsable technique

Le Responsable technique :

- enregistre les demandes reçues des clients ;
- qualifie les demandes ;
- valide ou modifie leur catégorie ;
- valide ou modifie leur priorité ;
- peut demander une analyse IA ;
- affecte les demandes aux Agents techniques ;
- peut réaffecter les demandes ;
- suit leur avancement ;
- examine les demandes déclarées résolues ;
- peut accepter une résolution et clôturer la demande ;
- peut refuser une résolution et remettre la demande en traitement ;
- peut annuler une demande lorsque cela est autorisé.

## 2.2 Agent technique

L'Agent technique :

- consulte les demandes qui lui sont affectées ;
- commence le traitement d'une demande ;
- renseigne la description du traitement ;
- renseigne la solution ;
- déclare la demande résolue.

Le rôle Agent technique est un rôle applicatif.
Il ne correspond pas nécessairement à un métier précis dans l'entreprise.

## 2.3 Administrateur

L'Administrateur :

- crée les comptes utilisateurs internes ;
- modifie les comptes ;
- active ou désactive les comptes ;
- attribue ou retire les rôles métier prévus.

Le rôle Administrateur ne donne pas automatiquement accès aux fonctions métier de gestion des demandes.

Le rôle ADMINISTRATEUR ne peut pas être attribué à un autre utilisateur depuis l'interface.

---

# 3. Permissions

| Action | Responsable technique | Agent technique | Administrateur |
|---|---:|---:|---:|
| Enregistrer une demande | Oui | Non | Non |
| Consulter toutes les demandes | Oui | Non | Non |
| Consulter ses demandes affectées | Oui si rôle Agent également | Oui | Non |
| Qualifier une demande | Oui | Non | Non |
| Valider/modifier la catégorie | Oui | Non | Non |
| Valider/modifier la priorité | Oui | Non | Non |
| Analyser avec l'IA | Oui | Non | Non |
| Affecter une demande | Oui | Non | Non |
| Réaffecter une demande | Oui | Non | Non |
| Commencer le traitement | Non sauf rôle Agent également | Oui | Non |
| Renseigner le traitement | Non sauf rôle Agent également | Oui | Non |
| Renseigner la solution | Non sauf rôle Agent également | Oui | Non |
| Déclarer une demande résolue | Non sauf rôle Agent également | Oui | Non |
| Refuser une résolution | Oui | Non | Non |
| Clôturer une demande | Oui | Non | Non |
| Annuler une demande | Oui | Non | Non |
| Gérer les utilisateurs | Non | Non | Oui |
| Attribuer les rôles métier | Non | Non | Oui |

Les permissions effectives d'un utilisateur correspondent à l'ensemble des rôles qui lui sont attribués.

---

# 4. Fonctionnalités V1

## Responsable technique

- Enregistrer une demande
- Qualifier une demande
- Analyser avec l'IA
- Gérer l'affectation d'une demande
- Suivre les demandes
- Examiner une demande résolue
- Annuler une demande

## Agent technique

- Consulter ses demandes affectées
- Traiter une demande

## Administrateur

- Gérer les utilisateurs
- Gérer les rôles métier

---

# 5. Règles métier

## 5.1 Demandes et clients

**RM01** — L'application est réservée aux utilisateurs internes de l'entreprise.

**RM02** — Le Client est une entité métier et n'est pas un utilisateur de l'application.

**RM03** — Une demande est créée/enregistrée par un Responsable technique.

**RM04** — Toute demande possède un client, une catégorie, une priorité et un statut.

**RM05** — Une nouvelle demande commence au statut `NOUVELLE`.

**RM06** — Lors de l'enregistrement d'une demande, le Responsable technique sélectionne un client déjà enregistré.

**RM07** — Si le client n'existe pas, ses informations minimales peuvent être renseignées pendant l'enregistrement de la demande.

**RM08** — Un client ainsi enregistré est conservé et peut être réutilisé pour d'autres demandes.

**RM09** — Une demande n'est pas supprimée fonctionnellement après sa création.

## 5.2 Catégorie

**RM10** — Une demande possède exactement une catégorie à un instant donné.

**RM11** — La catégorie appartient obligatoirement à la liste prédéfinie de la V1.

**RM12** — Aucune sous-catégorie n'est gérée en V1.

**RM13** — Le Responsable technique peut valider ou modifier la catégorie.

## 5.3 Priorité

**RM14** — Une demande possède une priorité parmi `BASSE`, `MOYENNE`, `HAUTE` et `CRITIQUE`.

**RM15** — `BASSE` signifie : faible urgence, la demande peut être traitée plus tard.

**RM16** — `MOYENNE` signifie : traitement normal.

**RM17** — `HAUTE` signifie : impact important ou échéance proche.

**RM18** — `CRITIQUE` signifie : prise en charge immédiate.

**RM19** — La priorité doit être évaluée selon le contexte réel de la demande.

**RM20** — La priorité ne découle pas automatiquement de la catégorie.

**RM21** — La priorité finale est validée ou définie par le Responsable technique.

## 5.4 Assistance IA

**RM22** — L'analyse IA est déclenchée explicitement par l'action « Analyser avec l'IA ».

**RM23** — L'IA propose uniquement une catégorie et une priorité.

**RM24** — La catégorie proposée par l'IA doit appartenir à la liste des catégories V1.

**RM25** — Une suggestion IA ne devient jamais automatiquement une décision métier finale.

**RM26** — Le Responsable technique doit valider ou modifier les suggestions de catégorie et de priorité.

**RM27** — L'indisponibilité ou l'échec de l'IA ne doit pas empêcher la gestion manuelle d'une demande.

## 5.5 Affectation et traitement

**RM28** — Une demande possède au maximum un Agent technique affecté à un instant donné.

**RM29** — Seul le Responsable technique peut affecter ou réaffecter une demande.

**RM30** — Une demande ne peut passer à `EN_COURS` que si un Agent technique lui est affecté.

**RM31** — Un Agent technique ne peut traiter que les demandes qui lui sont affectées.

**RM32** — L'Agent technique renseigne la description du traitement et la solution.

**RM33** — Une demande ne peut devenir `RESOLUE` que depuis `EN_COURS` et lorsqu'une solution a été renseignée.

**RM34** — Si une demande `EN_COURS` est réaffectée à un autre Agent technique, elle revient à `ASSIGNEE`.

**RM35** — Une réaffectation depuis `ASSIGNEE` conserve le statut `ASSIGNEE`.

## 5.6 Résolution et clôture

**RM36** — L'Agent technique déclare la demande résolue.

**RM37** — Seul le Responsable technique peut clôturer une demande résolue.

**RM38** — Le Responsable technique peut refuser une résolution et remettre la demande à `EN_COURS`.

## 5.7 Annulation

**RM39** — Seul le Responsable technique peut annuler une demande.

**RM40** — Une demande peut être annulée depuis `NOUVELLE`, `ASSIGNEE` ou `EN_COURS`.

**RM41** — Une demande `RESOLUE` ou `CLOTUREE` ne peut pas être annulée.

**RM42** — Toute annulation doit comporter un motif.

## 5.8 Traçabilité et rôles

**RM43** — Une demande `CLOTUREE` ou `ANNULEE` n'est plus modifiable fonctionnellement.

**RM44** — Les changements importants d'une demande doivent être historisés.

**RM45** — Un utilisateur peut posséder plusieurs rôles applicatifs.

**RM46** — Le rôle `ADMINISTRATEUR` ne donne aucun droit métier automatique sur les demandes.

**RM47** — Depuis l'interface d'administration, l'Administrateur peut attribuer ou retirer les rôles `RESPONSABLE_TECHNIQUE` et `AGENT_TECHNIQUE`.

**RM48** — Le rôle `ADMINISTRATEUR` ne peut pas être attribué depuis cette interface.

---

# 6. Cycle de vie d'une demande

États V1 :

- `NOUVELLE`
- `ASSIGNEE`
- `EN_COURS`
- `RESOLUE`
- `CLOTUREE`
- `ANNULEE`

Cycle nominal :

```text
NOUVELLE
   |
   | affectation
   v
ASSIGNEE
   |
   | début du traitement
   v
EN_COURS
   |
   | solution renseignée
   v
RESOLUE
   |
   | validation par le Responsable technique
   v
CLOTUREE
```

Retour après refus de résolution :

```text
RESOLUE -> EN_COURS
```

Réaffectation :

```text
ASSIGNEE -> ASSIGNEE
EN_COURS -> ASSIGNEE
```

Le nouvel Agent technique fait ensuite passer la demande à `EN_COURS` lorsqu'il commence réellement son traitement.

Annulation :

```text
NOUVELLE -> ANNULEE
ASSIGNEE -> ANNULEE
EN_COURS -> ANNULEE
```

`CLOTUREE` et `ANNULEE` sont des états terminaux.

---

# 7. Catégories V1

La liste des catégories est prédéfinie :

1. Étude de dangers
2. Analyse de risques industriels
3. Protection incendie
4. Note de calcul
5. Dossier technique
6. Assistance réglementaire
7. Autre

Aucune sous-catégorie n'est prévue dans la V1.

---

# 8. Priorités V1

| Priorité | Signification |
|---|---|
| `BASSE` | Faible urgence, peut être traitée plus tard |
| `MOYENNE` | Traitement normal |
| `HAUTE` | Impact important ou échéance proche |
| `CRITIQUE` | Nécessite une prise en charge immédiate |

La catégorie d'une demande ne détermine pas automatiquement sa priorité.

---

# 9. Place de l'IA

L'IA est une fonction d'assistance.

Elle est déclenchée par le bouton :

**Analyser avec l'IA**

Elle peut uniquement proposer :

- une catégorie ;
- une priorité.

Le Responsable technique reste responsable de la décision finale.

Le module IA interne n'est pas considéré comme un acteur UML.

---

# 10. Éléments explicitement hors scope V1

Ne font pas partie de la V1 :

- portail ou compte Client ;
- Client comme acteur logiciel ;
- module autonome « Gérer les clients » ;
- sous-catégories ;
- statut `EN_ATTENTE` ;
- système de notes ou commentaires ;
- suppression fonctionnelle d'une demande ;
- attribution du rôle `ADMINISTRATEUR` depuis l'interface ;
- classification ou priorisation IA sans validation humaine ;
- résumé automatique par IA ;
- affectation automatique d'une demande par l'IA.
