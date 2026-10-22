# ByOp

**ByOp** es un plugin para servidores de Minecraft que controla quién puede recibir el rol de **operador (OP)**. Bloquea el comando `/op` para cualquier nick que no esté autorizado, tanto si lo ejecuta un jugador como la consola o RCON, y puede retirar el OP automáticamente a quien no esté en la lista.

La lista de autorizados se edita **manualmente** en `plugins/ByOp/allowed.txt`. No hay comandos para agregar o quitar nicks: tú decides quién entra escribiendo su nick en el archivo.

| Dato | Valor |
| --- | --- |
| Nombre | ByOp |
| Versión | 1.2.0 |
| Autor | **CamiKiss** |
| Plataforma | Bukkit, Spigot, Paper |
| Versiones de Minecraft | 1.7 – 26.3 (última versión) |
| Java | 8 o superior (Paper 26.1+ requiere Java 25) |
| Dependencias | Ninguna |

---

## Índice

1. [Características](#características)
2. [Requisitos y compatibilidad](#requisitos-y-compatibilidad)
3. [Instalación](#instalación)
4. [Primeros pasos](#primeros-pasos)
5. [La lista de autorizados](#la-lista-de-autorizados)
6. [Comandos y permisos](#comandos-y-permisos)
7. [Configuración](#configuración)
8. [Cómo funciona](#cómo-funciona)
9. [Seguridad y recomendaciones](#seguridad-y-recomendaciones)
10. [Compilar desde el código](#compilar-desde-el-código)
11. [Estructura del proyecto](#estructura-del-proyecto)
12. [Solución de problemas](#solución-de-problemas)
13. [Historial de versiones](#historial-de-versiones)
14. [Créditos](#créditos)

---

## Características

- **Bloquea `/op` a jugadores no autorizados.** Si un jugador intenta dar OP a un nick que no está en la lista, el comando se cancela y recibe un mensaje.
- **Bloquea `/op` desde consola y RCON.** El comando no llega a ejecutarse si el nick no está autorizado.
- **Compatible con `/minecraft:op`.** También detecta el comando con namespace y con barras repetidas.
- **Retira el OP automáticamente.** Si `deop-unauthorized` está activo, cualquier jugador con OP que no esté autorizado pierde el OP al entrar y en cada revisión periódica.
- **Lista manual y transparente.** Un nick por línea en `allowed.txt`; sin base de datos ni comandos de administración.
- **Comparación sin distinguir mayúsculas.** `CamiKiss`, `camikiss` y `CAMIKISS` cuentan como el mismo nick.
- **Mensajes personalizables.** Textos, prefijo, colores y placeholders configurables desde `config.yml`.
- **Sin dependencias.** No necesita Vault, LuckPerms ni ningún otro plugin.
- **Código ligero.** Sin NMS: usa solo la API de Bukkit, por lo que funciona en muchas versiones.

---

## Requisitos y compatibilidad

- Servidor **Bukkit, Spigot o Paper** de Minecraft **1.7 a 26.3**, incluida la última versión estable de Paper.
- **Java 8 o superior**. El plugin está compilado con bytecode de **Java 8**, pero cada servidor exige su propia versión de Java: Paper 26.1 y posteriores requieren **Java 25**.
- Probado en **Paper 1.21.1** con Java 21 y en **Paper 26.3 build 211** con Java 25.
- No requiere conexión a base de datos ni plugins adicionales.

> En versiones muy antiguas que no tienen el evento de comandos de consola, ByOp avisa en la consola de que no puede bloquear `/op` desde ahí. El bloqueo de comandos de jugadores sigue funcionando.

---

## Instalación

1. Descarga o compila `ByOp-1.2.0.jar`.
2. Coloca el archivo en la carpeta `plugins/` del servidor.
3. Reinicia el servidor.
4. Abre `plugins/ByOp/allowed.txt` y escribe los nicks autorizados, uno por línea.
5. Ejecuta `/byop reload` o reinicia el servidor.

Al arrancar por primera vez, el plugin crea automáticamente:

```
plugins/ByOp/
├── config.yml
└── allowed.txt
```

Si tu servidor usa una carpeta de plugins distinta, los archivos se crean dentro de la carpeta de datos del plugin.

---

## Primeros pasos

Ejemplo rápido para autorizar a dos jugadores:

```txt
CamiKiss
Steve
```

Después de guardar el archivo:

```txt
/byop reload
/byop list
```

Salida esperada:

```txt
[ByOp] Lista recargada. Nicks permitidos: 2
[ByOp] Nicks permitidos (2): CamiKiss, Steve
```

A partir de ese momento:

- `/op CamiKiss` funciona.
- `/op Steve` funciona.
- `/op Cualquiera` se bloquea.
- Si un jugador no autorizado tiene OP, se le retira automáticamente.

---

## La lista de autorizados

El archivo principal es:

```
plugins/ByOp/allowed.txt
```

Reglas:

- Escribe **un nick por línea**.
- Las líneas que empiezan con `#` son comentarios.
- Se permiten líneas vacías.
- Se puede escribir un comentario después del nick, separado por un espacio.
- La comparación no distingue mayúsculas de minúsculas.
- Si la lista está vacía, **nadie** puede recibir OP.
- Los cambios se aplican con `/byop reload` o al reiniciar.

Ejemplo completo:

```txt
# ByOp - nicks autorizados
CamiKiss
Steve
# Alex           <- comentado, no autorizado
Notch           # administrador
```

En el ejemplo anterior están autorizados `CamiKiss`, `Steve` y `Notch`. `Alex` no lo está porque su línea empieza con `#`.

### Dar o quitar autorización

1. Abre `allowed.txt`.
2. Agrega o elimina la línea del nick.
3. Guarda el archivo.
4. Ejecuta `/byop reload`.

No existe `/byop add` ni `/byop remove`: la edición del archivo es el único método.

---

## Comandos y permisos

| Comando | Descripción |
| --- | --- |
| `/byop reload` | Recarga `config.yml` y vuelve a leer `allowed.txt`. |
| `/byop list` | Muestra los nicks cargados actualmente y su cantidad. |
| `/byop` | Muestra la ayuda con el uso correcto. |

| Permiso | Descripción | Por defecto |
| --- | --- | --- |
| `byop.admin` | Permite usar `/byop`. | `op` |

El permiso se puede asignar con tu plugin de permisos habitual (LuckPerms, PermissionsEx, etc.).

> `/byop list` muestra la lista **cargada en memoria**. Si acabas de editar `allowed.txt`, ejecuta `/byop reload` antes de comprobarla.

---

## Configuración

Archivo:

```
plugins/ByOp/config.yml
```

Contenido por defecto:

```yaml
# ByOp - by CamiKiss
# La lista de nicks autorizados se edita MANUALMENTE en allowed.txt
# (plugins/ByOp/allowed.txt), un nick por linea. No hay comandos para agregar.
# Despues de editar allowed.txt ejecuta /byop reload.
#
# Si esta en true, se quita el OP a cualquier jugador que no este en la lista
# (se revisa al entrar y cada check-interval-ticks).
deop-unauthorized: true

# Cada cuantos ticks se revisa quien tiene OP sin estar autorizado (20 ticks = 1 segundo).
check-interval-ticks: 40

messages:
  prefix: "&4[ByOp] &c"
  no-permission: "&cNo tienes permiso para usar este comando."
  usage: "&cUso: /byop reload | list"
  op-blocked-player: "&cNo puedes dar OP a &f%target%&c. Ese nick no esta autorizado."
  op-blocked-console: "&cComando /op bloqueado: &f%target%&c no esta autorizado."
  op-removed: "&cTu OP fue retirado: no estas autorizado."
  reloaded: "&aLista recargada. Nicks permitidos: &f%count%"
  list: "&eNicks permitidos (&f%count%&e): &f%names%"
  empty-names: "&7ninguno"
```

### Opciones

| Opción | Tipo | Por defecto | Descripción |
| --- | --- | --- | --- |
| `deop-unauthorized` | booleano | `true` | Si es `true`, retira el OP a los jugadores que no estén autorizados. |
| `check-interval-ticks` | número | `40` | Cada cuántos ticks se revisan los jugadores con OP. 20 ticks = 1 segundo. El mínimo es 20. |
| `messages.*` | texto | — | Mensajes del plugin. |

### Mensajes

| Clave | Cuándo se muestra |
| --- | --- |
| `prefix` | Se antepone a todos los mensajes. |
| `no-permission` | El emisor no tiene `byop.admin`. |
| `usage` | Se usa `/byop` sin argumentos o con uno inválido. |
| `op-blocked-player` | Un jugador intenta dar OP a un nick no autorizado. |
| `op-blocked-console` | Consola o RCON intenta dar OP a un nick no autorizado. |
| `op-removed` | Se retira el OP automáticamente a un jugador. |
| `reloaded` | Se ejecuta `/byop reload` correctamente. |
| `list` | Se ejecuta `/byop list`. |
| `empty-names` | La lista está vacía al usar `/byop list`. |

### Colores y placeholders

- Usa `&` para los colores: `&c` rojo, `&a` verde, `&e` amarillo, `&l` negrita, etc.
- Placeholders disponibles:

| Placeholder | Significado |
| --- | --- |
| `%target%` | Nick al que se intenta dar OP o nick afectado. |
| `%player%` | Jugador que ejecuta o recibe la acción. |
| `%count%` | Cantidad de nicks autorizados. |
| `%names%` | Lista de nicks autorizados separados por coma. |

Ejemplo:

```yaml
messages:
  prefix: "&8[&bByOp&8] &r"
  op-blocked-console: "&cAcceso denegado para &f%target%&c."
```

---

## Cómo funciona

1. **Intercepción de comandos de jugador.** ByOp escucha `PlayerCommandPreprocessEvent` y analiza el comando antes de que se ejecute.
2. **Intercepción de consola y RCON.** Escucha `ServerCommandEvent` y cancela el comando si el destino no está autorizado.
3. **Lectura del destino.** Se reconoce `/op Nick`, `op Nick`, `/minecraft:op Nick` y variantes con barras adicionales.
4. **Comprobación de la lista.** El nick se compara sin distinguir mayúsculas con los cargados desde `allowed.txt`.
5. **Bloqueo.** Si no está autorizado, el comando se cancela y se envía el mensaje configurado.
6. **Retirada automática de OP.** Un temporizador recorre a los jugadores conectados y quita el OP a quien no esté autorizado. También se comprueba al entrar.
7. **Recarga.** `/byop reload` vuelve a leer la configuración y el archivo de nicks.

> ByOp no impide editar `ops.json` a mano ni que otro plugin conceda OP. La revisión automática está pensada para corregir esos casos si `deop-unauthorized` está activo.

---

## Seguridad y recomendaciones

- Mantén `allowed.txt` fuera del alcance de personas no confiables: quien pueda editarlo puede autorizar su propio nick.
- No des el permiso `byop.admin` a cualquiera; solo controla `/byop`, pero es una buena práctica limitarlo.
- Usa `/byop reload` después de cada edición para aplicar los cambios sin reiniciar.
- Si desactivas `deop-unauthorized`, el plugin seguirá bloqueando `/op` a nicks no autorizados, pero no retirará el OP que ya tengan.
- Ten en cuenta que un jugador con OP puede hacer mucho daño. ByOp es una capa de control, no un sistema de permisos completo.
- Si usas RCON, protege la contraseña y el puerto; ByOp bloquea el comando, pero la seguridad del acceso remoto depende de tu configuración.

---

## Compilar desde el código

El proyecto incluye un `pom.xml` para compilar con Maven:

```bash
mvn clean package
```

El jar se genera en:

```
target/ByOp-1.2.0.jar
```

### Compilación manual (sin Maven)

Necesitas la API de Spigot en el classpath. Ejemplo en Windows:

```powershell
$api = "ruta\a\spigot-api-1.8.8-R0.1-SNAPSHOT.jar"
New-Item -ItemType Directory -Force -Path target\classes | Out-Null
javac --release 8 -encoding UTF-8 -cp $api -d target\classes src\main\java\com\byop\*.java
Copy-Item src\main\resources\plugin.yml,src\main\resources\config.yml,src\main\resources\allowed.txt target\classes\
jar cf ByOp-1.2.0.jar -C target\classes .
```

El resultado es un plugin sin dependencias externas.

---

## Estructura del proyecto

```
ByOp/
├── pom.xml
├── README.md
├── ByOp-1.2.0.jar
└── src/
    └── main/
        ├── java/
        │   └── com/byop/
        │       ├── ByOpPlugin.java
        │       └── ServerCommandListener.java
        └── resources/
            ├── plugin.yml
            ├── config.yml
            └── allowed.txt
```

- `ByOpPlugin.java`: lógica principal, lista, mensajes, comandos y tareas.
- `ServerCommandListener.java`: bloqueo de `/op` desde consola y RCON.
- `plugin.yml`: metadatos, comando y permiso.
- `config.yml`: opciones y mensajes.
- `allowed.txt`: lista manual de nicks autorizados.

---

## Solución de problemas

| Problema | Posible causa y solución |
| --- | --- |
| El plugin no aparece en `/plugins` | Comprueba que el jar está en `plugins/` y que la versión de Java es compatible. Revisa la consola al arrancar. |
| `/op Nick` sigue funcionando | Verifica que el nick está escrito correctamente en `allowed.txt` y ejecuta `/byop reload`. Recuerda que la comparación ignora mayúsculas, pero no espacios. |
| Un jugador autorizado pierde el OP | Revisa que su nick esté en la lista y que no haya líneas comentadas. Ejecuta `/byop list`. |
| Un jugador no autorizado conserva el OP | Asegúrate de que `deop-unauthorized: true` y espera `check-interval-ticks` (por defecto 2 segundos). |
| No se bloquea `/op` desde consola | En versiones muy antiguas sin `ServerCommandEvent`, el bloqueo de consola no está disponible; la consola mostrará un aviso. |
| Los cambios no se aplican | Guarda `allowed.txt` y ejecuta `/byop reload`. |
| No sé qué nicks están cargados | Ejecuta `/byop list`. |
| El archivo no existe | Reinicia el servidor; el plugin lo crea con una plantilla. |

---

## Historial de versiones

### 1.2.0
- La lista se administra **solo manualmente** en `allowed.txt`.
- Eliminados los comandos `add`, `remove` e `import`.
- Eliminado el soporte de MySQL/MariaDB y el driver incluido.
- Créditos a CamiKiss.
- Jar más ligero, sin dependencias.
- Compatibilidad comprobada en Paper 1.21.1 (Java 21) y Paper 26.3 (Java 25).

### 1.1.0
- Soporte experimental de lista compartida con MySQL/MariaDB.
- Comandos para agregar, quitar e importar nicks.

### 1.0.0
- Primera versión: bloqueo de `/op` y retirada automática de OP.

---

## Créditos

**ByOp** fue creado por **CamiKiss**.

- Autor y créditos: **CamiKiss**
- Idea y desarrollo del plugin: **CamiKiss**
- Agradecimiento a la comunidad de Bukkit, Spigot y Paper por su API.

Si usas o redistribuyes este plugin, conserva los créditos a CamiKiss.
