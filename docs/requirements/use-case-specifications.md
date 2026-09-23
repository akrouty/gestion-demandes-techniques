# Fiches détaillées des cas d'utilisation — V1

**Statut : DRAFT**

Ce document détaille les cas d'utilisation à partir du périmètre fonctionnel V1 validé.

## 1. Enregistrer une demande

### Acteur principal

Responsable technique.

### Objectif

Enregistrer une demande reçue d'un client avec une catégorie, une priorité et le statut initial `NOUVELLE`.

### Préconditions

- Le Responsable technique est connecté.

### Scénario nominal

1. Le Responsable technique renseigne les informations de la demande.
2. Il sélectionne un client déjà enregistré.
3. Il qualifie la demande en définissant ou validant sa catégorie et sa priorité.
4. Il enregistre la demande.
5. Le système conserve la demande avec le statut `NOUVELLE`.

### Scénarios alternatifs réellement prévus

- Si le client n'existe pas, le Responsable technique renseigne ses informations minimales pendant l'enregistrement. Le client est conservé et devient réutilisable.
- Le Responsable technique peut utiliser l'assistance IA pendant la qualification. Son utilisation reste facultative et sa validation humaine reste obligatoire.

### Postconditions

- La demande est enregistrée avec un client, une catégorie, une priorité et le statut `NOUVELLE`.
- Le client nouvellement renseigné, le cas échéant, est conservé.

## 2. Qualifier une demande

### Acteur principal

Responsable technique.

### Objectif

Définir ou modifier la catégorie et la priorité d'une demande.

### Préconditions

- Le Responsable technique est connecté.
- La demande est en cours d'enregistrement ou reste fonctionnellement modifiable.

### Scénario nominal

1. Le Responsable technique examine le contexte de la demande.
2. Il choisit une catégorie dans la liste prédéfinie de la V1.
3. Il évalue la priorité selon le contexte réel de la demande.
4. Il valide la catégorie et la priorité.

### Scénarios alternatifs réellement prévus

- Le Responsable technique modifie ultérieurement la catégorie ou la priorité d'une demande encore modifiable.
- Il peut demander une analyse IA avant de valider ou modifier les valeurs proposées.

### Postconditions

- La demande possède exactement une catégorie V1 et une priorité parmi `BASSE`, `MOYENNE`, `HAUTE` et `CRITIQUE`.
- Les valeurs finales correspondent à la décision du Responsable technique.

## 3. Analyser avec l'IA

### Acteur principal

Responsable technique.

### Objectif

Obtenir une proposition de catégorie et de priorité pour assister la qualification d'une demande.

### Préconditions

- Le Responsable technique est connecté.
- Les informations de la demande nécessaires à sa qualification ont été renseignées.

### Scénario nominal

1. Le Responsable technique déclenche l'action « Analyser avec l'IA ».
2. Le système analyse la demande.
3. Le système propose une catégorie appartenant à la liste V1 et une priorité.
4. Le Responsable technique valide ou modifie les deux propositions.

### Scénarios alternatifs réellement prévus

- Si l'analyse IA échoue ou est indisponible, le Responsable technique poursuit la qualification manuellement.

### Postconditions

- En cas de succès, les propositions IA sont présentées au Responsable technique.
- La catégorie et la priorité ne deviennent définitives qu'après validation humaine.
- Un échec de l'IA ne bloque pas la qualification ni l'enregistrement.

## 4. Gérer l'affectation d'une demande

### Acteur principal

Responsable technique.

### Objectif

Affecter ou réaffecter une demande à un Agent technique.

### Préconditions

- Le Responsable technique est connecté.
- La demande reste fonctionnellement modifiable.

### Scénario nominal

1. Le Responsable technique choisit un Agent technique.
2. Il affecte la demande à cet Agent.
3. Le système conserve l'affectation.
4. Le système place la demande au statut `ASSIGNEE`.

### Scénarios alternatifs réellement prévus

- Si une demande `ASSIGNEE` est réaffectée, elle reste au statut `ASSIGNEE`.
- Si une demande `EN_COURS` est réaffectée, elle revient au statut `ASSIGNEE`. Le nouvel Agent la repasse à `EN_COURS` lorsqu'il commence réellement le traitement.

### Postconditions

- La demande possède au maximum un Agent technique affecté.
- Son statut respecte les règles d'affectation ou de réaffectation.

## 5. Suivre les demandes

### Acteur principal

Responsable technique.

### Objectif

Consulter les demandes et suivre leur avancement.

### Préconditions

- Le Responsable technique est connecté.

### Scénario nominal

1. Le Responsable technique consulte les demandes.
2. Le système présente leur état courant.
3. Le Responsable technique consulte l'avancement d'une demande.

### Scénarios alternatifs réellement prévus

Aucun scénario alternatif spécifique n'est validé pour la V1.

### Postconditions

- Aucune donnée métier n'est modifiée par la consultation.

## 6. Examiner une demande résolue

### Acteur principal

Responsable technique.

### Objectif

Accepter ou refuser la résolution d'une demande déclarée `RESOLUE`.

### Préconditions

- Le Responsable technique est connecté.
- La demande est au statut `RESOLUE`.

### Scénario nominal

1. Le Responsable technique examine la demande résolue et sa solution.
2. Il accepte la résolution.
3. Le système place la demande au statut `CLOTUREE`.

### Scénarios alternatifs réellement prévus

- Si le Responsable technique refuse la résolution, le système remet la demande au statut `EN_COURS` afin que l'Agent technique puisse reprendre le traitement.

### Postconditions

- La demande est `CLOTUREE` si la résolution est acceptée.
- La demande est `EN_COURS` si la résolution est refusée.

## 7. Annuler une demande

### Acteur principal

Responsable technique.

### Objectif

Annuler une demande tout en conservant sa traçabilité.

### Préconditions

- Le Responsable technique est connecté.
- La demande est au statut `NOUVELLE`, `ASSIGNEE` ou `EN_COURS`.

### Scénario nominal

1. Le Responsable technique demande l'annulation.
2. Il renseigne le motif d'annulation.
3. Le système place la demande au statut `ANNULEE`.

### Scénarios alternatifs réellement prévus

- Une demande `RESOLUE` ou `CLOTUREE` ne peut pas être annulée.
- Sans motif d'annulation, la demande ne passe pas au statut `ANNULEE`.

### Postconditions

- La demande est au statut terminal `ANNULEE` et n'est plus modifiable fonctionnellement.
- La demande n'est pas supprimée.

## 8. Consulter ses demandes affectées

### Acteur principal

Agent technique.

### Objectif

Consulter les demandes qui lui sont affectées.

### Préconditions

- L'Agent technique est connecté.

### Scénario nominal

1. L'Agent technique demande à consulter ses demandes affectées.
2. Le système présente les demandes qui lui sont affectées.
3. L'Agent technique consulte une demande.

### Scénarios alternatifs réellement prévus

- Si aucune demande ne lui est affectée, aucune demande n'est présentée.

### Postconditions

- Aucune donnée métier n'est modifiée par la consultation.

## 9. Traiter une demande

### Acteur principal

Agent technique.

### Objectif

Traiter une demande affectée et la déclarer résolue après avoir renseigné une solution.

### Préconditions

- L'Agent technique est connecté.
- La demande lui est affectée.
- La demande est au statut `ASSIGNEE` ou `EN_COURS`.

### Scénario nominal

1. L'Agent technique commence le traitement d'une demande `ASSIGNEE`.
2. Le système place la demande au statut `EN_COURS`.
3. L'Agent technique renseigne la description du traitement.
4. Il renseigne la solution.
5. Il déclare la demande résolue.
6. Le système place la demande au statut `RESOLUE`.

### Scénarios alternatifs réellement prévus

- Pour une demande déjà `EN_COURS`, l'Agent technique poursuit ou reprend le traitement.
- Si aucune solution n'est renseignée, la demande ne peut pas passer au statut `RESOLUE`.
- Après le refus d'une résolution, l'Agent technique reprend le traitement de la demande remise à `EN_COURS`.

### Postconditions

- La demande est `EN_COURS` tant que son traitement se poursuit.
- Après déclaration d'une résolution comportant une solution, la demande est `RESOLUE`.

## 10. Gérer les utilisateurs

### Acteur principal

Administrateur.

### Objectif

Créer, modifier, activer ou désactiver les comptes des utilisateurs internes.

### Préconditions

- L'Administrateur est connecté.

### Scénario nominal

1. L'Administrateur consulte les utilisateurs internes.
2. Il crée ou modifie un compte utilisateur.
3. Il active ou désactive le compte selon le besoin.
4. Le système conserve les changements.

### Scénarios alternatifs réellement prévus

Aucun scénario alternatif spécifique n'est validé pour la V1.

### Postconditions

- Le compte utilisateur est créé ou mis à jour avec son état d'activation courant.

## 11. Gérer les rôles métier

### Acteur principal

Administrateur.

### Objectif

Attribuer ou retirer les rôles métier prévus aux utilisateurs internes.

### Préconditions

- L'Administrateur est connecté.
- L'utilisateur interne concerné existe.

### Scénario nominal

1. L'Administrateur choisit un utilisateur.
2. Il attribue ou retire le rôle `RESPONSABLE_TECHNIQUE` ou `AGENT_TECHNIQUE`.
3. Le système conserve les rôles de l'utilisateur.

### Scénarios alternatifs réellement prévus

- Le rôle `ADMINISTRATEUR` ne peut pas être attribué depuis l'interface.
- Un même utilisateur peut cumuler plusieurs rôles applicatifs.

### Postconditions

- Les rôles métier de l'utilisateur correspondent aux attributions conservées par le système.
- Les permissions effectives de l'utilisateur correspondent à l'ensemble de ses rôles.

## 12. Se connecter

### Acteur principal

Responsable technique, Agent technique ou Administrateur.

### Objectif

Accéder à l'application interne selon les rôles attribués à l'utilisateur.

### Préconditions

- L'utilisateur dispose d'un compte interne actif.

### Scénario nominal

1. L'utilisateur demande à se connecter.
2. Il fournit ses informations de connexion.
3. Le système reconnaît l'utilisateur et ses rôles.
4. Le système ouvre la session et donne accès aux fonctions autorisées par ces rôles.

### Scénarios alternatifs réellement prévus

- Si l'utilisateur ne peut pas être reconnu ou si son compte est désactivé, la session n'est pas ouverte.

### Postconditions

- Une session est ouverte pour l'utilisateur reconnu avec les permissions correspondant à ses rôles, ou aucun accès n'est accordé.
