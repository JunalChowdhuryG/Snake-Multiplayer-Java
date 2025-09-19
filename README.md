# Snake Multijugador en Java

Este proyecto es una implementación del clásico juego **Snake**, adaptado a un entorno **multijugador en red** con **programación concurrente** en Java.  
Cada jugador controla su propia serpiente y compite contra otros para comer frutas, crecer y evitar choques.  

---

## Funcionamiento actual

1. Colocar todos los archivos `.java` en una misma carpeta.
2. Compilar el proyecto desde la terminal:
```bash
javac *.java
```

3. Ejecutar el servidor en una terminal:

```bash
java SnakeServer
```
4. En otra terminal, ejecutar el cliente:

```bash
java SnakeClient
```

---

## Controles del juego

* El movimiento se realiza escribiendo la tecla y luego presionando **Enter**:

  * `w` + Enter → Arriba
  * `s` + Enter → Abajo
  * `a` + Enter → Izquierda
  * `d` + Enter → Derecha

> Por el momento, el juego solo admite este modo de control. En futuras versiones se mejorará la interacción en tiempo real.

---

## Archivos principales

* **Board.java** → Representación del tablero de juego.
* **Game.java** → Contiene la lógica principal del juego.
* **PlayerHandler.java** → Maneja las conexiones de los jugadores en red.
* **Snake.java** → Define la estructura, movimiento y crecimiento de cada serpiente.
* **SnakeClient.java** → Cliente que se conecta al servidor para jugar.
* **SnakeServer.java** → Servidor que gestiona múltiples jugadores.


