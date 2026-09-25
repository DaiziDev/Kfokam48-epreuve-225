# Design system

## Benchmark — d'où vient chaque décision

| Référence | Ce qu'on a retenu | Ce qu'on a écarté |
|---|---|---|
| **Linear** | Bordures fines 1px, densité aérée, typographie serrée, palette restreinte | Le mode sombre (hors périmètre v0.1) |
| **Stripe Dashboard** | Indigo primaire + neutres slate ; la **typo mono comme élément de design** (notre code à 6 caractères est le héros de l'app) | Les illustrations complexes |
| **shadcn/ui** | Tokens sémantiques, classes composées `.btn`/`.card`, élévation douce | La dépendance à Radix (Angular ≠ React) |
| **Atlassian Design** | Discipline de nommage : token sémantique (`--color-success`) avant décoratif (`#059669`) | La densité extrême des tables |
| **Kits attendance/éducation** (Figma, Dribbble) | Gros CTA pleine largeur sur mobile, **couleurs de statut** (vert/ambre/rouge), layout en cartes | Les gradients décoratifs et les icônes remplies |

**Positionnement retenu** : « sérieux mais chaleureux ». Outil d'évaluation (notes,
verrouillage) : la confiance passe par la sobriété ; outil quotidien d'étudiants sur
mobile : la chaleur passe par un primaire indigo vivant et des statuts lisibles.

## Tokens (`src/styles.css`, `@theme` Tailwind 4)

| Famille | Tokens | Usage |
|---|---|---|
| **Brand** | `brand-50…950` (indigo curaté) | Primaire : CTA, liens actifs, kicker, focus |
| **Neutres** | palette `slate` Tailwind | Texte, bordures, surfaces |
| **Sémantiques** | `success` (émeraude), `warning` (ambre), `danger` (rouge) + variantes `-soft`/`-border` | Statuts de présence, d'exercice, de session (EF16) |
| **Typo** | `font-sans` (Inter/system), `font-mono` (SF Mono/Cascadia/Consolas) | UI / **codes de présence** |
| **Rayon** | `radius-card: 0.75rem` | Toutes les cartes |
| **Élévation** | `shadow-card` (1px doux), `shadow-pop` (flottants) | Jamais d'ombre lourde : la bordure porte la structure |

### Règles de couleur

1. **Un seul primaire** : tout ce qui est cliquable-important est `brand-600`, hover `brand-700`, active `brand-800`.
2. **Le sémantique ne ment jamais** : vert = acquis (présence enregistrée, exercice relu), ambre = en attente, rouge = refus/expiré. Jamais de rouge décoratif.
3. **375px d'abord** (ENF1) : chaque écran vit dans `max-w-md` (étudiant/relecteur) ou `max-w-3xl` (formateur), padding `px-4`, cibles tactiles ≥ 44px.

## Icônes — Lucide, zéro emoji

- Bibliothèque **Lucide** (licence ISC), SVG **inlinés** dans un registre maison
  (`ui/icon/lucide.ts`) consommé par `<ui-icon name="…">`.
- **Pourquoi inline plutôt que la librairie npm** : zéro dépendance à synchroniser avec
  les versions Angular, tree-shaking garanti (seules les icônes déclarées existent),
  trait 2px identique partout.
- **Interdiction stricte d'emoji** dans l'UI — ils rendent différemment selon l'OS,
  ne s'héritent pas de la couleur, et ne sont pas accessibles.
- Icônes du produit : `graduation-cap` (marque), `scan-line` (étudiant),
  `clipboard-check` (relecteur), `table-2` (formateur), `calendar-plus` (session),
  `check-circle-2` (succès), `alert` (attention), `globe` (langue).

## Internationalisation FR/EN

- `I18nService` à base de **signals** : `{{ 'clé' | t }}` dans les templates,
  bascule instantanée sans rechargement, bouton drapeau-texte `FR⇄EN` dans l'en-tête
  (`<ui-icon name="globe">`).
- Dictionnaire plat clé → texte ; le **français est la référence** (le client parle français).
- Les textes métier (erreurs API) restent en français côté backend : le format du
  contrat impose un `message` lisible ; l'UI les affiche tels quels.

## Composants UI (`src/app/ui/`)

| Composant | Rôle |
|---|---|
| `ui-icon` | SVG Lucide inline, taille = taille de police |
| `ui-badge-statut` | Badge sémantique (success/warning/danger/neutral/brand) + icône |
| `ui-code-input` | **Pièce maîtresse** : 6 cases mono, filtrage alphanumérique, avance auto, Backspace intelligent, collage réparti, flèches, CVA |

## Accessibilité

- Anneau de focus unique (`:focus-visible`, brand-500) sur tout le produit.
- `CodeInput` : `role="group"` + label par case, `autocomplete="one-time-code"`.
- Icônes `aria-hidden` (décoratives), boutons avec `aria-label` traduit.
- Contraste : texte principal `slate-900` sur `slate-50` (ratio > 15:1), blanc sur `brand-600` (4.6:1).
