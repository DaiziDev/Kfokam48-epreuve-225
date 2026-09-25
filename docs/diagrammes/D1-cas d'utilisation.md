@startuml D1-cas-utilisation
left to right direction

actor Formateur
actor Étudiant
actor Système

rectangle "Système KFOKAM48" {
    usecase "Ouvrir une session" as UC1
    usecase "Clôturer une session" as UC2
    usecase "Rouvrir une session" as UC3
    usecase "Ajouter une présence manuelle" as UC4
    usecase "Consulter le tableau de suivi" as UC5

    usecase "Marquer sa présence avec un code" as UC6
    usecase "Déposer le lien de son exercice" as UC7
    usecase "Remplacer le lien de son exercice" as UC8
    usecase "Effectuer une relecture assignée" as UC9
    usecase "Consulter sa note et son commentaire" as UC10

    usecase "Assigner deux relecteurs distincts au hasard" as UC11
}

Formateur --> UC1
Formateur --> UC2
Formateur --> UC3
Formateur --> UC4
Formateur --> UC5

Étudiant --> UC6
Étudiant --> UC7
Étudiant --> UC8
Étudiant --> UC9
Étudiant --> UC10

Système --> UC11
@enduml
