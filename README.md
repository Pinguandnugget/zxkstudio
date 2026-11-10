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
| `/cp dar inmunosupresor <jugadores> [cantidad]` | Da el Inmunosupresor |
| `/cp dar bloqueador <jugadores> [cantidad]` | Da el Bloqueador Neural |
| `/cp recargar` / `/cp restablecer` | Relee el archivo / valores por defecto |

Los valores se guardan en `config/zxkstudio_ciberpsicosis.json`.

**Modo solo_bajas:** mientras esté activo, beber un Inmunosupresor o un Bloqueador estando
en cyberpsychosis no sirve (el ítem no se gasta). Al terminar el evento, `/cp modo normal`
y vuelven a curar como siempre. El Inmunosupresor se craftea: botella de cristal, 2 lingotes
de hierro, redstone y lapislázuli (sin forma).

## Notas

- Morir cura la infección (igual que en el datapack).
- La receta del antídoto usa objetos de Alex's Caves. Si ese mod no está, la receta no
  carga, pero lo demás funciona y el antídoto se puede dar con `/sj antidoto`.
- Los tags de dinosaurios están en `data/zxkstudio/tags/entity_type/sangre_jurasica/`
  y se pueden cambiar con un datapack.
- No dejes instalados los datapacks `sangre_jurasica` ni `ciberpsicosis` originales junto con este mod: harían lo mismo dos veces.
