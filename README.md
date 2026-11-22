# zxkstudio — Fabric 1.21.1

Mod en Java con dos eventos, ambos **siempre activos** (no hay comando para apagarlos):

- **Sangre Jurásica**: los dinosaurios te infectan al golpearte, tienes un tiempo para beber
  el **Antídoto de Ámbar** y, si no lo haces, entras en fase terminal.
- **Cyberpsychosis**: un reloj de inmunosupresores; si se agota entras en cyberpsychosis y
  te estabilizas matando (bajas) o con un **Inmunosupresor**.

Son la versión en código de los datapacks `sangre_jurasica` y `ciberpsicosis`.

> Este código está escrito pero **todavía no se ha compilado ni probado en el juego**.
> Si al compilar sale algún error, pégaselo a Claude y se corrige.

## Compilarlo en GitHub (sin instalar nada)

1. Crea un repositorio público en GitHub y sube **todo el contenido de esta carpeta**
   (incluida la carpeta oculta `.github`).
2. Entra a la pestaña **Actions**. Se ejecuta solo en cada subida; también puedes lanzarlo
   con **Run workflow**.
3. Cuando termine, abre la ejecución y descarga **zxkstudio-mod** en *Artifacts*.
   Dentro está el `.jar` (usa el que NO termina en `-sources`).
4. Si sale en rojo, abre el paso **Compilar**, copia el error y pásaselo a Claude.

## Compilarlo en tu PC

Necesitas **Java 21** (JDK) instalado.

1. Entra a https://fabricmc.net/develop y genera una plantilla: Minecraft **1.21.1**,
   mappings **Yarn**, Java 21. Descomprímela.
2. De la plantilla, copia la carpeta `gradle/` y los archivos `gradlew` y `gradlew.bat`
   dentro de esta carpeta (para tener el wrapper de Gradle).
3. Abre `gradle.properties` de la plantilla y compara con el de aquí. Si Gradle se queja de
   alguna versión, usa las de la plantilla: `loader_version`, `loom_version`, `fabric_version`.
4. En esta carpeta ejecuta `./gradlew build` (en Windows: `gradlew.bat build`).
5. El mod queda en `build/libs/zxkstudio-1.0.0.jar` (no uses el que termina en `-sources`).

Para jugar necesitas **Fabric Loader** y **Fabric API** para 1.21.1. El mod tiene que estar
instalado en el servidor y también en los clientes.

## Comandos

Todos requieren ser operador. `/sj` es lo mismo que `/sangrejurasica`.
Sin valor, cada comando muestra el valor actual.

| Comando | Qué ajusta |
|---|---|
| `/sj estado` | Muestra todos los valores |
| `/sj tiempo <segundos>` | Duración de la fase 1 (por defecto 300) |
| `/sj dano <valor>` | Daño de cada golpe en fase terminal (2.0 = 1 corazón) |
| `/sj intervalo <segundos>` | Cada cuántos segundos llega ese daño (por defecto 1) |
| `/sj veneno <nivel>` | Veneno en fase terminal, 0 = sin veneno (por defecto 0) |
| `/sj debilidad <nivel>` | Debilidad, 0 = apagada (por defecto 1) |
| `/sj lentitud <nivel>` | Lentitud, 0 = apagada (por defecto 1) |
| `/sj nausea <true/false>` | Náuseas |
| `/sj oscuridad <true/false>` | Oscuridad |
| `/sj latido <true/false>` | Sonido de latido |
| `/sj particulas <true/false>` | Partículas |
| `/sj mensajes <true/false>` | Mensajes en el chat |
| `/sj provocar <true/false>` | Los dinosaurios hostiles atacan de lejos |
| `/sj radio <bloques>` | Radio de esa provocación (por defecto 20) |
| `/sj infectar <jugadores>` | Infecta a los jugadores indicados |
| `/sj curar <jugadores>` | Cura a los jugadores indicados |
| `/sj antidoto <jugadores>` | Da el Antídoto de Ámbar |
| `/sj recargar` | Vuelve a leer el archivo de configuración |
| `/sj restablecer` | Deja todo con los valores por defecto |

Los valores se guardan en `config/zxkstudio_sangre_jurasica.json`.

## Comandos de Cyberpsychosis

Requieren ser operador. `/cp` es lo mismo que `/ciberpsicosis`. Los jugadores en creativo,
espectador o con el tag `cp_inmune` no se ven afectados.

| Comando | Qué hace |
|---|---|
| `/inmunosupresores` | Cualquier jugador: ve su tiempo o sus bajas |
| `/cp estado` | Muestra todos los valores |
| `/cp entrar <jugadores>` | Mete a los jugadores en cyberpsychosis |
| `/cp curar <jugadores>` | Los cura y reinicia el reloj |
| `/cp reiniciar <jugadores>` | Reloj completo, sin inmunidad |
| `/cp reloj <jugadores> <segundos>` | Fija el reloj (para probar el aviso final) |
| `/cp modo normal` | Los Inmunosupresores curan la cyberpsychosis |
| `/cp modo solo_bajas` | NO se puede curar con ítems: solo matando `bajas` entidades |
| `/cp tiempo <segundos>` | Reloj de inmunosupresores (por defecto 900) |
| `/cp bajas <n>` | Bajas necesarias (por defecto 6) |
| `/cp dano <valor>` | Daño de cada pop (por defecto 4.0; `1000` = muerte, como el datapack) |
| `/cp popmin` / `/cp popmax <segundos>` | Cada cuánto llega el pop (9 a 12) |
| `/cp bloqueador <segundos>` | Duración de la inmunidad del Bloqueador Neural |
| `/cp overlays` / `figuras` / `chatfalso` / `sacudidas <true/false>` | Efectos visuales |
| `/cp alucinaciones <true/false>` | Alucinaciones de playa y nieve en cyberpsychosis |
| `/cp alucinar <jugadores> playa\|nieve` | Fuerza una alucinación (el jugador debe estar en cyberpsychosis) |
| `/cp alucinacion <segundos>` | Duración antes del susto (por defecto 30) |
| `/cp esperamin` / `/cp esperamax <segundos>` | Espera entre alucinaciones (45 a 120) |
| `/cp dar inmunosupresor <jugadores> [cantidad]` | Da el Inmunosupresor |
| `/cp dar bloqueador <jugadores> [cantidad]` | Da el Bloqueador Neural |
| `/cp recargar` / `/cp restablecer` | Relee el archivo / valores por defecto |

Los valores se guardan en `config/zxkstudio_ciberpsicosis.json`.

**Modo solo_bajas:** mientras esté activo, beber un Inmunosupresor o un Bloqueador estando
en cyberpsychosis no sirve (el ítem no se gasta). Al terminar el evento, `/cp modo normal`
y vuelven a curar como siempre. El Inmunosupresor y el Bloqueador se usan como una inyección (sin gesto de beber): clic del inyector, pistón, pinchazo con sacudida de cámara, burbujeo, latido y chispas. El Inmunosupresor se craftea: botella de cristal, 2 lingotes
de hierro, redstone y lapislázuli (sin forma).

## Alucinaciones

En cyberpsychosis el jugador "viaja" de vez en cuando a una **playa** o a una **montaña nevada**.
Se le envían chunks modificados solo a él (hasta ~7 chunks a la redonda): suelo de arena o de nieve,
bioma de playa o helado (cielo, niebla, hierba), hora, ventisca y sonidos. El mundo real no cambia.

- Al empezar la escena se quitan los efectos de la cyberpsychosis (náuseas, oscuridad, fuerza,
  velocidad) y no hay pops; siguen en cyberpsychosis. Al despertar vuelven todos los efectos.
- Una figura que solo ve él se acerca cuando no la mira: **Ahogado** (playa), **Warden** (nieve)
  o la **Sombra** (un Illusioner con piel negra y ojos rojos, más alto; la piel está en
  `assets/minecraft/textures/entity/illager/illusioner.png`). Al final, un susto sin daño.
- Durante la escena no puede romper ni colocar bloques. Si lo curas, termina al instante.

## Notas

- Morir cura la infección (igual que en el datapack).
- La receta del antídoto usa objetos de Alex's Caves. Si ese mod no está, la receta no
  carga, pero lo demás funciona y el antídoto se puede dar con `/sj antidoto`.
- Los tags de dinosaurios están en `data/zxkstudio/tags/entity_type/sangre_jurasica/`
  y se pueden cambiar con un datapack.
- No dejes instalados los datapacks `sangre_jurasica` ni `ciberpsicosis` originales junto con este mod: harían lo mismo dos veces.

## PotitoSirenia (Árbol de Sirenia)

Todo usa el namespace `zxkstudio:`. El código está en `com.zxkstudio.sirenia` y se arranca con una
sola línea desde `ZxkStudio`, así que no altera Sangre Jurásica ni Cyberpsychosis.

**Cómo se cultiva** (no se planta ningún árbol, solo crece el fruto):

1. Encuentra un Árbol de Sirenia (manglares, raro; costas cálidas, muy raro; cuevas frondosas, excepcional).
2. Pela un tronco con un hacha.
3. Clic derecho con un **Germen de Sirenia** en una cara lateral del tronco pelado. Hace falta que la base
   del árbol esté sobre barro y que haya 4 fuentes de agua a 3 bloques o menos. Si falta algo, no se gasta la semilla.
4. Cada tronco admite hasta 4 injertos (uno por cara). Crecen solos: injerto → pequeño (10 min) → maduro (25) →
   marchito (55) → podrido (70), en minutos de tiempo cargado. Con condiciones inválidas el crecimiento se pausa.
5. Polvo de hueso: 2 para fruto pequeño, 5 en total para maduro. No funciona en frutos maduros, marchitos ni podridos.
6. Clic derecho para cosechar. El tronco no se rompe y la cara queda libre para otra semilla.

Los frutos siguen envejeciendo en inventarios y contenedores (la fecha va en el componente `zxkstudio:fruit_clock`,
así que dividir o mover stacks no la reinicia).

**Apilado de frutos**: se apilan solo los de la misma fase (maduro con maduro, etc.), aunque tengan minutos distintos de vida.
Cada fruto nace con su propio reloj al cosecharse. Al unir stacks, el resultado conserva el reloj del stack de destino.
Cuando un stack llega a su siguiente fase, pasa a ser del objeto de esa fase y deja de apilarse con los de la anterior.

**Efectos**:
- Corazón maduro: Sirenomorphosis 180 s (3 min). Respiras, minas y nadas mejor bajo el agua y ves más lejos
  (niebla del original de Dedsafio 3). **Fuera del agua te ahogas** (aire y daño de ahogamiento).
  Cura la Podredumbre de Sirenia.
- Corazón marchito: Sirenomorphosis 60 s. No cura la Podredumbre.
- Corazón podrido: **Podredumbre de Sirenia** (`zxkstudio:sirenia_rot`): pantalla teñida de cian, corazones celestes y
  daño constante que nunca mata. Dura hasta curarse comiendo un Corazón de Sirenia maduro (la leche no la cura).
  Morir también la quita.

**Creativo**: todo el contenido de zxkstudio está en la pestaña **zxkstudio** (Sirenia, Antídoto de Ámbar,
Inmunosupresor y Bloqueador Neural).

**Configuración**: `config/zxkstudio_sirenia.json` (se crea al primer arranque; los cambios se aplican al reiniciar).
Incluye tiempos, agua, duraciones, `land_penalty_enabled` (ahogo fuera del agua, por defecto `true`),
minería/velocidad/visión/partículas submarinas, niebla en tierra (`land_fog_enabled`), la Podredumbre
(`rot_effect_duration_seconds`, 0 = hasta curarse) y la frecuencia de generación (`mangrove_chance`, `coast_chance`,
`lush_cave_chance`, `generation_enabled`). Si el archivo viene de la primera versión, se migra solo a los valores nuevos
(3 min y ahogo activado).

**Mixins** (solo cliente, ambos con `require = 0`: si otro mod cambia el método, simplemente no se aplican):
`BackgroundRendererMixin` (niebla y color de Sirenomorphosis) y `HeartTypeMixin` (corazones celestes de la Podredumbre).

Como hay bloques, texturas y modelos propios, el mod debe estar en el servidor y en los clientes.

## Talasofobia (miedo al agua profunda)

Apagada por defecto. Un operador la activa con `/talasofobia on` (y `off`); también `/talasofobia estado` y
`/talasofobia curar <jugadores>`.

1. Con la cabeza bajo el agua y 12 bloques de agua o más bajo los pies, tras 10 s seguidos aparece el efecto **Talasofobia**.
2. Tienes **45 s** para reaccionar (cuenta atrás en el HUD). En los últimos 10 s empiezan las alucinaciones: sonidos
   fuertes del fondo del mar, un tiburón fantasma que se acerca y "muerde" (golpe de cámara y sonido, sin quitar vida),
   el susto del Guardián Anciano y la pantalla que se oscurece.
3. Al llegar a 0 empieza el **pánico**: cada 4 s recibes un golpe letal que gasta un tótem (o te mata).
4. **Cura**: salir del agua o comer un Corazón de Sirenia maduro o marchito. Con Sirenomorphosis activa eres inmune.
   La leche o el tótem no la quitan; morir sí.

Todo lo falso solo lo ve el jugador afectado y no hay entidades reales (el "tiburón" son paquetes con el modelo del delfín).
Se ajusta con las opciones `thalassophobia_*` del archivo de configuración.
