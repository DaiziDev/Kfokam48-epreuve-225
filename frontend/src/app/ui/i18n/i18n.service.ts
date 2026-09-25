import { Injectable, computed, signal } from '@angular/core';

export type Locale = 'fr' | 'en';

/** Dictionnaires plat : clé → texte. Le français est la langue de référence. */
const DICTIONNAIRES: Record<Locale, Record<string, string>> = {
  fr: {
    'app.titre': 'KFOKAM48 — Présence & Relecture',

    'accueil.badge': 'Outil officiel de la formation KFOKAM48',
    'accueil.titre-1': 'La présence en',
    'accueil.mot-code': 'un code.',
    'accueil.titre-2': 'La relecture entre pairs.',
    'accueil.sous-titre':
      'Le formateur ouvre une session, projette un code à 6 caractères, les étudiants pointent depuis leur téléphone. Les exercices déposés sont relus par les pairs, au hasard parmi les présents.',
    'accueil.cta-primaire': 'Marquer ma présence',
    'accueil.cta-secondaire': 'Espace formateur',

    'accueil.stats-titre': 'Des règles simples, appliquées à la lettre',
    'accueil.stat-1': 'minutes de validité du code',
    'accueil.stat-2': 'relecteur par exercice, au hasard parmi les présents',
    'accueil.stat-3': 'relectures en attente rendues visibles',
    'accueil.stat-4': "d'attente après 5 codes erronés",

    'accueil.comment-titre': 'Trois étapes, aucune feuille de présence',
    'accueil.etape-1-titre': 'Le formateur ouvre la session',
    'accueil.etape-1-texte': 'Un code à 6 caractères est généré, valable 15 minutes, projeté en salle.',
    'accueil.etape-2-titre': 'Les étudiants pointent',
    'accueil.etape-2-texte': 'Chacun saisit le code depuis son téléphone. Les doubles présences sont refusées automatiquement.',
    'accueil.etape-3-titre': 'Les pairs relisent',
    'accueil.etape-3-texte': 'Chaque exercice déposé reçoit un relecteur au hasard parmi les présents. Note de 0 à 20, définitive.',

    'accueil.garanties-titre': 'Conçu pour être juste, même quand personne ne regarde',
    'accueil.garantie-1-titre': 'Relecteur toujours distinct de l\'auteur',
    'accueil.garantie-1-texte': 'Le système ne peut jamais assigner un exercice à son propre auteur.',
    'accueil.garantie-2-titre': 'Anonymat du relecteur',
    'accueil.garantie-2-texte': 'L étudiant relu voit sa note et son commentaire, jamais le nom du relecteur.',
    'accueil.garantie-3-titre': 'Note verrouillée dès l\'envoi',
    'accueil.garantie-3-texte': 'Une relecture rendue est définitive : aucune correction possible ensuite.',
    'accueil.garantie-4-titre': 'Double présence impossible',
    'accueil.garantie-4-texte': 'La base de données elle-même refuse deux présences du même étudiant à une session.',
    'accueil.garantie-5-titre': 'Clôture automatique après 24 h',
    'accueil.garantie-5-texte': 'Une session oubliée se clôt seule un jour après l\'expiration du code.',
    'accueil.garantie-6-titre': 'Rattrapage par le formateur',
    'accueil.garantie-6-texte': 'Un étudiant sans réseau au moment du code ? Le formateur ajoute sa présence à la main.',

    'accueil.cta-final-texte': 'Formateur ou étudiant, votre session vous attend.',
    'accueil.cta-final-primaire': 'Pointer maintenant',
    'accueil.cta-final-secondaire': 'Voir le tableau de suivi',

    'accueil.pied': 'Présence par code éphémère · Relecture entre pairs · Notes verrouillées',

    'accueil.mockup-etiquette': 'Code de présence',
    'accueil.mockup-succes': 'Présence enregistrée',
    'nav.etudiant': 'Étudiant',
    'nav.relecteur': 'Relecteur',
    'nav.formateur': 'Formateur',
    'nav.langue': 'Changer de langue',
    'nav.theme': 'Basculer entre thème clair et sombre',

    'etudiant.kicker': 'Espace étudiant',
    'etudiant.titre': 'Marquez votre présence',
    'etudiant.tagline':
      'Saisissez le code à 6 caractères projeté par le formateur pour enregistrer votre présence à la session.',
    'etudiant.etiquette-code': 'Code de présence',
    'etudiant.placeholder': 'Ex. JQP4AP',
    'etudiant.action': 'Valider ma présence',
    'etudiant.succes': 'Présence enregistrée. Bonne session !',
    'erreur.CODE_INCONNU': 'Ce code ne correspond à aucune session. Vérifiez la saisie.',
    'erreur.CODE_EXPIRE': 'Ce code a expiré : la fenêtre de 15 minutes est passée.',
    'erreur.DEJA_PRESENT': 'Vous êtes déjà marqué présent à cette session.',
    'erreur.SESSION_CLOTUREE': 'La session est clôturée : il n est plus possible de pointer.',
    'erreur.ETUDIANT_INCONNU': 'Étudiant inconnu.',
    'erreur.RESEAU_INJOIGNABLE': 'Impossible de joindre le serveur. Vérifiez votre connexion.',
    'erreur.defaut': 'Une erreur inattendue est survenue.',
    'etudiant.en-attente': "Dépôt d'exercice et relectures arrivent avec les prochains tickets.",

    'relecteur.kicker': 'Espace relecteur',
    'relecteur.titre': 'Vos relectures assignées',
    'relecteur.tagline':
      "Les exercices qui vous ont été confiés pour relecture, note et commentaire à l'appui.",
    'relecteur.en-attente': 'Disponible avec les tickets EF9 à EF12.',

    'formateur.kicker': 'Espace formateur',
    'formateur.titre': 'Ouvrez une session',
    'formateur.tagline':
      'Créez la session, partagez le code à 6 caractères, puis suivez les présences et les relectures en direct.',
    'formateur.en-attente': 'Création de session (EF1) et tableau de suivi (EF16) arrivent ici.',
    'formateur.etiquette-titre': 'Titre de la session',
    'formateur.placeholder-titre': 'Ex. Algorithmique — séance 1',
    'formateur.action': 'Ouvrir la session',
    'formateur.session-ouverte': 'Session ouverte',
    'formateur.code-a-partager': 'Projetez ce code aux étudiants — ils le saisissent pour pointer.',
    'formateur.expire-dans': 'Le code expire dans',
    'formateur.minutes': 'min',
  },
  en: {
    'app.titre': 'KFOKAM48 — Attendance & Peer Review',

    'accueil.badge': 'The official tool of the KFOKAM48 program',
    'accueil.titre-1': 'Attendance in',
    'accueil.mot-code': 'one code.',
    'accueil.titre-2': 'Peer review among classmates.',
    'accueil.sous-titre':
      'The instructor opens a session and projects a 6-character code; students check in from their phones. Submitted exercises are reviewed by peers, drawn at random among those present.',
    'accueil.cta-primaire': 'Check in now',
    'accueil.cta-secondaire': 'Instructor area',

    'accueil.stats-titre': 'Simple rules, enforced to the letter',
    'accueil.stat-1': 'minutes of code validity',
    'accueil.stat-2': 'reviewer per exercise, drawn at random among attendees',
    'accueil.stat-3': 'pending reviews kept visible',
    'accueil.stat-4': 'lockout after 5 wrong codes',

    'accueil.comment-titre': 'Three steps, no attendance sheet',
    'accueil.etape-1-titre': 'The instructor opens the session',
    'accueil.etape-1-texte': 'A 6-character code is generated, valid for 15 minutes, projected in the room.',
    'accueil.etape-2-titre': 'Students check in',
    'accueil.etape-2-texte': 'Each student types the code from their phone. Duplicate check-ins are rejected automatically.',
    'accueil.etape-3-titre': 'Peers review',
    'accueil.etape-3-texte': 'Every submitted exercise gets a reviewer drawn at random among attendees. Grade 0-20, final.',

    'accueil.garanties-titre': 'Designed to be fair, even when nobody is watching',
    'accueil.garantie-1-titre': 'Reviewer never the author',
    'accueil.garantie-1-texte': 'The system can never assign an exercise to its own author.',
    'accueil.garantie-2-titre': 'Reviewer anonymity',
    'accueil.garantie-2-texte': "The reviewed student sees their grade and comment, never the reviewer's name.",
    'accueil.garantie-3-titre': 'Grade locked on submission',
    'accueil.garantie-3-texte': 'A submitted review is final: no corrections afterwards.',
    'accueil.garantie-4-titre': 'Duplicate check-in impossible',
    'accueil.garantie-4-texte': 'The database itself refuses two check-ins from the same student in one session.',
    'accueil.garantie-5-titre': 'Auto-close after 24 h',
    'accueil.garantie-5-texte': 'A forgotten session closes itself one day after the code expires.',
    'accueil.garantie-6-titre': 'Instructor catch-up',
    'accueil.garantie-6-texte': 'No signal when the code was shown? The instructor adds the attendance by hand.',

    'accueil.cta-final-texte': 'Instructor or student, your session is waiting.',
    'accueil.cta-final-primaire': 'Check in now',
    'accueil.cta-final-secondaire': 'See the tracking dashboard',

    'accueil.pied': 'Ephemeral code check-in · Peer review · Locked grades',

    'accueil.mockup-etiquette': 'Attendance code',
    'accueil.mockup-succes': 'Attendance recorded',
    'nav.etudiant': 'Student',
    'nav.relecteur': 'Reviewer',
    'nav.formateur': 'Instructor',
    'nav.langue': 'Switch language',
    'nav.theme': 'Toggle between light and dark theme',

    'etudiant.kicker': 'Student area',
    'etudiant.titre': 'Check in to your session',
    'etudiant.tagline':
      'Enter the 6-character code shown by your instructor to record your attendance.',
    'etudiant.etiquette-code': 'Attendance code',
    'etudiant.placeholder': 'e.g. JQP4AP',
    'etudiant.action': 'Check in',
    'etudiant.succes': 'Attendance recorded. Enjoy your session!',
    'erreur.CODE_INCONNU': 'This code matches no session. Please double-check.',
    'erreur.CODE_EXPIRE': 'This code has expired: the 15-minute window has passed.',
    'erreur.DEJA_PRESENT': 'You are already checked in to this session.',
    'erreur.SESSION_CLOTUREE': 'The session is closed: check-in is no longer possible.',
    'erreur.ETUDIANT_INCONNU': 'Unknown student.',
    'erreur.RESEAU_INJOIGNABLE': 'Cannot reach the server. Check your connection.',
    'erreur.defaut': 'An unexpected error occurred.',
    'etudiant.en-attente': 'Exercise submission and peer reviews arrive with upcoming tickets.',

    'relecteur.kicker': 'Reviewer area',
    'relecteur.titre': 'Your assigned reviews',
    'relecteur.tagline':
      'Exercises assigned to you for review, with grade and comment.',
    'relecteur.en-attente': 'Available with tickets EF9 to EF12.',

    'formateur.kicker': 'Instructor area',
    'formateur.titre': 'Open a session',
    'formateur.tagline':
      'Create the session, share the 6-character code, then track attendance and reviews live.',
    'formateur.en-attente': 'Session creation (EF1) and tracking dashboard (EF16) land here.',
    'formateur.etiquette-titre': 'Session title',
    'formateur.placeholder-titre': 'e.g. Algorithms — session 1',
    'formateur.action': 'Open the session',
    'formateur.session-ouverte': 'Session is open',
    'formateur.code-a-partager': 'Project this code — students type it to check in.',
    'formateur.expire-dans': 'Code expires in',
    'formateur.minutes': 'min',
  },
};

/**
 * i18n minimaliste à base de signals : le changement de langue est réactif
 * dans toute l'application, sans dépendance externe. FR par défaut.
 */
@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly localeCourante = signal<Locale>('fr');

  readonly locale = this.localeCourante.asReadonly();

  readonly estFrancais = computed(() => this.localeCourante() === 'fr');

  traduire(cle: string): string {
    const dictionnaire = DICTIONNAIRES[this.localeCourante()];
    return dictionnaire[cle] ?? DICTIONNAIRES['fr'][cle] ?? cle;
  }

  basculer(): void {
    this.localeCourante.update((locale) => (locale === 'fr' ? 'en' : 'fr'));
  }
}
