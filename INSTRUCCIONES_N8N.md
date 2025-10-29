# Instrucciones para Configurar Evaluación Automática en n8n

## 📋 Descripción General

Este documento explica cómo configurar un workflow en n8n para evaluar automáticamente el Trabajo Práctico Integrador de Programación 2, utilizando la rúbrica definida en `RUBRICA_N8N.json`.

---

## 🎯 Objetivo

Procesar un archivo único que contenga todas las clases Java del proyecto y generar automáticamente:
1. Puntaje por categoría (0-110 puntos)
2. Observaciones específicas por criterio
3. Recomendaciones de mejora
4. Nota final y resultado (Aprobado/Desaprobado)

---

## 📁 Formato del Archivo de Entrada

### Estructura Requerida

El estudiante debe entregar un único archivo `.txt` o `.java` con todas las clases separadas por delimitadores:

```java
// ===== CLASE: Config/DatabaseConnection.java =====
package Config;

import java.sql.Connection;
import java.sql.DriverManager;
// ... resto del código ...

// ===== CLASE: Models/Base.java =====
package Models;

public abstract class Base {
    private int id;
    // ... resto del código ...

// ===== CLASE: Models/Persona.java =====
package Models;

public class Persona extends Base {
    private String nombre;
    // ... resto del código ...

// ... y así sucesivamente para todas las clases
```

### Orden Sugerido de Clases

1. **Config/** (2 clases)
   - DatabaseConnection.java
   - TransactionManager.java

2. **Models/** (3 clases)
   - Base.java
   - Persona.java
   - Domicilio.java

3. **Dao/** (4 clases)
   - GenericDAO.java
   - PersonaDAO.java
   - DomicilioDAO.java

4. **Service/** (4 clases)
   - GenericService.java
   - PersonaServiceImpl.java
   - DomicilioServiceImpl.java

5. **Main/** (4 clases)
   - Main.java
   - AppMenu.java
   - MenuHandler.java
   - MenuDisplay.java

**Total esperado**: 15-17 archivos Java

---

## 🔧 Configuración del Workflow en n8n

### Nodos Requeridos

1. **Webhook / File Upload Node**
2. **Function Node - Extractor de Clases**
3. **Function Node - Evaluador por Categoría**
4. **Function Node - Calculador de Puntaje**
5. **Set Node - Formatear Salida**
6. **HTTP Response / Email Node - Enviar Resultados**

### Diagrama del Workflow

```
[Webhook/Upload]
    ↓
[Leer Archivo]
    ↓
[Cargar RUBRICA_N8N.json]
    ↓
[Extraer Clases] → Separa por delimitador
    ↓
[Evaluar Categoría 1: Arquitectura] ──┐
[Evaluar Categoría 2: Persistencia] ──┤
[Evaluar Categoría 3: Recursos]     ──┼→ [Agregar Resultados]
[Evaluar Categoría 4: Validación]   ──┤
[Evaluar Categoría 5: Funcionalidad]──┘
    ↓
[Evaluar Bonificaciones]
    ↓
[Evaluar Penalizaciones]
    ↓
[Calcular Puntaje Final]
    ↓
[Determinar Nota y Resultado]
    ↓
[Formatear Salida JSON]
    ↓
[Enviar Respuesta HTTP / Email]
```

---

## 💻 Código para Nodos n8n

### 1. Function Node - Extractor de Clases

```javascript
// Extrae las clases del archivo subido
const fileContent = $input.item.binary.data.data;
const decodedContent = Buffer.from(fileContent, 'base64').toString('utf-8');

// Separar por delimitador
const delimiter = /\/\/ ={5,} CLASE: (.+?) ={5,}/g;
const classes = [];
let match;
let lastIndex = 0;

while ((match = delimiter.exec(decodedContent)) !== null) {
  if (lastIndex > 0) {
    const previousClass = classes[classes.length - 1];
    previousClass.content = decodedContent.substring(lastIndex, match.index).trim();
  }

  classes.push({
    name: match[1].trim(),
    path: match[1].trim(),
    content: '',
    startIndex: match.index
  });

  lastIndex = delimiter.lastIndex;
}

// Última clase
if (classes.length > 0 && lastIndex > 0) {
  classes[classes.length - 1].content = decodedContent.substring(lastIndex).trim();
}

return {
  json: {
    totalClasses: classes.length,
    classes: classes,
    fullContent: decodedContent,
    extractedAt: new Date().toISOString()
  }
};
```

### 2. Function Node - Evaluador de Categoría (Ejemplo: Arquitectura)

```javascript
// Cargar la rúbrica (asumir que está en $node["Cargar Rubrica"].json.rubrica)
const rubrica = $node["Cargar Rubrica"].json.rubrica;
const classes = $input.item.json.classes;
const fullContent = $input.item.json.fullContent;

// Función auxiliar para contar ocurrencias de patrón
function contarPatron(patron, texto, flags = 'g') {
  const regex = new RegExp(patron, flags);
  const matches = texto.match(regex);
  return matches ? matches.length : 0;
}

// Función auxiliar para verificar si existe patrón
function existePatron(patron, texto, flags = '') {
  const regex = new RegExp(patron, flags);
  return regex.test(texto);
}

// Evaluar Categoría 1: Arquitectura y Diseño
const categoria = rubrica.categorias.find(c => c.id === 1);
let puntajeCategoria = 0;
let observaciones = [];

// Subcategoría 1.1: Separación en Capas
const paquetes = new Set();
classes.forEach(cls => {
  const match = cls.content.match(/package\s+([\w.]+);/);
  if (match) {
    const paquete = match[1].split('.')[0]; // Primer nivel
    paquetes.add(paquete);
  }
});

if (paquetes.size >= 5) {
  puntajeCategoria += 10;
  observaciones.push(`✅ Excelente: ${paquetes.size} paquetes detectados (${Array.from(paquetes).join(', ')})`);
} else if (paquetes.size >= 4) {
  puntajeCategoria += 8;
  observaciones.push(`✓ Muy bueno: ${paquetes.size} paquetes detectados`);
} else if (paquetes.size >= 3) {
  puntajeCategoria += 6;
  observaciones.push(`⚠ Bueno: Solo ${paquetes.size} paquetes detectados`);
} else {
  puntajeCategoria += 4;
  observaciones.push(`❌ Insuficiente: Solo ${paquetes.size} paquetes detectados. Se esperan al menos 5`);
}

// Verificar dependencias inversas
let violacionesDependencia = 0;
classes.forEach(cls => {
  const packageMatch = cls.content.match(/package\s+([\w.]+);/);
  if (packageMatch) {
    const pkg = packageMatch[1].split('.')[0];

    // Verificar imports incorrectos
    if (pkg === 'Dao' && cls.content.includes('import Service.')) {
      violacionesDependencia++;
      observaciones.push(`❌ DAO no debe importar Service: ${cls.name}`);
    }
    if (pkg === 'Models' && (cls.content.includes('import Dao.') || cls.content.includes('import Service.'))) {
      violacionesDependencia++;
      observaciones.push(`❌ Models no debe importar capas superiores: ${cls.name}`);
    }
  }
});

if (violacionesDependencia > 0) {
  puntajeCategoria -= violacionesDependencia * 3;
  observaciones.push(`⚠ ${violacionesDependencia} violación(es) de dependencia detectadas (-${violacionesDependencia * 3} puntos)`);
}

// Subcategoría 1.2: Patrones de Diseño
const patronesDetectados = {
  dao: contarPatron('class\\s+\\w+DAO\\s+implements\\s+GenericDAO', fullContent),
  sqlConstantes: contarPatron('private\\s+static\\s+final\\s+String\\s+(SELECT|INSERT|UPDATE|DELETE)_', fullContent),
  serviceLayer: contarPatron('class\\s+\\w+ServiceImpl\\s+implements', fullContent),
  factory: existePatron('public\\s+static\\s+\\w+\\s+(getConnection|getInstance)\\(', fullContent)
};

let puntajePatrones = 0;
if (patronesDetectados.dao >= 2) {
  puntajePatrones += 3;
  observaciones.push(`✅ Patrón DAO implementado: ${patronesDetectados.dao} clases DAO`);
}
if (patronesDetectados.sqlConstantes >= 8) {
  puntajePatrones += 3;
  observaciones.push(`✅ Queries SQL como constantes: ${patronesDetectados.sqlConstantes} constantes`);
} else if (patronesDetectados.sqlConstantes >= 5) {
  puntajePatrones += 2;
  observaciones.push(`✓ Queries SQL como constantes: ${patronesDetectados.sqlConstantes} constantes (parcial)`);
}
if (patronesDetectados.serviceLayer >= 2) {
  puntajePatrones += 2;
  observaciones.push(`✅ Service Layer implementado: ${patronesDetectados.serviceLayer} servicios`);
}
if (patronesDetectados.factory) {
  puntajePatrones += 2;
  observaciones.push(`✅ Factory Pattern detectado`);
}

puntajeCategoria += puntajePatrones;

// Subcategoría 1.3: POO
const pooCriterios = {
  abstraccion: contarPatron('(abstract\\s+class|interface)\\s+\\w+', fullContent),
  atributosPrivados: contarPatron('private\\s+\\w+\\s+\\w+;', fullContent),
  atributosPublicos: contarPatron('public\\s+(?!static\\s+final|class|interface|void|\\w+\\s+get|\\w+\\s+set)\\w+\\s+\\w+;', fullContent),
  sobrescritura: contarPatron('@Override\\s+(public|protected)\\s+(boolean\\s+equals|int\\s+hashCode|String\\s+toString)\\(', fullContent),
  gettersSetters: contarPatron('(public\\s+\\w+\\s+get|public\\s+void\\s+set)\\w+\\(', fullContent)
};

let puntajePOO = 0;
if (pooCriterios.abstraccion >= 2) {
  puntajePOO += 3;
  observaciones.push(`✅ Herencia/Abstracción: ${pooCriterios.abstraccion} clases abstractas/interfaces`);
}
if (pooCriterios.atributosPrivados >= 10) {
  puntajePOO += 3;
  observaciones.push(`✅ Encapsulamiento correcto: ${pooCriterios.atributosPrivados} atributos privados`);
} else {
  puntajePOO += Math.min(3, Math.floor(pooCriterios.atributosPrivados / 3));
  observaciones.push(`⚠ Encapsulamiento parcial: ${pooCriterios.atributosPrivados} atributos privados (se esperan al menos 10)`);
}
if (pooCriterios.atributosPublicos > 0) {
  const penalizacion = pooCriterios.atributosPublicos * 2;
  puntajePOO -= penalizacion;
  observaciones.push(`❌ Atributos públicos detectados: ${pooCriterios.atributosPublicos} (-${penalizacion} puntos)`);
}
if (pooCriterios.sobrescritura >= 3) {
  puntajePOO += 2;
  observaciones.push(`✅ Sobrescritura de métodos: ${pooCriterios.sobrescritura} métodos`);
}
if (pooCriterios.gettersSetters >= 8) {
  puntajePOO += 2;
  observaciones.push(`✅ Getters/Setters implementados: ${pooCriterios.gettersSetters} métodos`);
}

puntajeCategoria += puntajePOO;

// Limitar puntaje máximo de la categoría
puntajeCategoria = Math.min(puntajeCategoria, categoria.puntaje_maximo);

return {
  json: {
    categoriaId: 1,
    categoriaNombre: categoria.nombre,
    puntajeObtenido: puntajeCategoria,
    puntajeMaximo: categoria.puntaje_maximo,
    porcentaje: (puntajeCategoria / categoria.puntaje_maximo * 100).toFixed(2),
    observaciones: observaciones,
    detalles: {
      paquetes: Array.from(paquetes),
      patronesDetectados: patronesDetectados,
      pooCriterios: pooCriterios,
      violacionesDependencia: violacionesDependencia
    }
  }
};
```

### 3. Function Node - Evaluador de Bonificaciones

```javascript
const rubrica = $node["Cargar Rubrica"].json.rubrica;
const fullContent = $node["Extractor de Clases"].json.fullContent;

function contarPatron(patron, texto, flags = 'g') {
  const regex = new RegExp(patron, flags);
  const matches = texto.match(regex);
  return matches ? matches.length : 0;
}

let bonificacionTotal = 0;
let bonificacionesObtenidas = [];

rubrica.bonificaciones.forEach(bonus => {
  let cumple = false;

  if (bonus.verificacion.patron) {
    const ocurrencias = contarPatron(bonus.verificacion.patron, fullContent);
    if (ocurrencias >= bonus.verificacion.minimo_ocurrencias) {
      cumple = true;
      bonificacionTotal += bonus.puntos;
      bonificacionesObtenidas.push({
        id: bonus.id,
        nombre: bonus.nombre,
        puntos: bonus.puntos,
        detalle: `${ocurrencias} ocurrencias detectadas (mínimo: ${bonus.verificacion.minimo_ocurrencias})`
      });
    }
  } else if (bonus.verificacion.patrones_multiples) {
    let patronesEncontrados = 0;
    bonus.verificacion.patrones_multiples.forEach(patron => {
      if (new RegExp(patron).test(fullContent)) {
        patronesEncontrados++;
      }
    });

    if (patronesEncontrados >= bonus.verificacion.minimo) {
      cumple = true;
      bonificacionTotal += bonus.puntos;
      bonificacionesObtenidas.push({
        id: bonus.id,
        nombre: bonus.nombre,
        puntos: bonus.puntos,
        detalle: `${patronesEncontrados}/${bonus.verificacion.patrones_multiples.length} patrones implementados`
      });
    }
  }
});

return {
  json: {
    bonificacionTotal: bonificacionTotal,
    bonificacionesObtenidas: bonificacionesObtenidas,
    maxBonificacion: 10
  }
};
```

### 4. Function Node - Evaluador de Penalizaciones

```javascript
const rubrica = $node["Cargar Rubrica"].json.rubrica;
const fullContent = $node["Extractor de Clases"].json.fullContent;

function contarPatron(patron, texto, flags = 'g') {
  const regex = new RegExp(patron, flags);
  const matches = texto.match(regex);
  return matches ? matches.length : 0;
}

let penalizacionTotal = 0;
let penalizacionesAplicadas = [];

rubrica.penalizaciones.forEach(penalizacion => {
  const ocurrencias = contarPatron(penalizacion.verificacion.patron, fullContent);

  if (ocurrencias > 0) {
    // Si la penalización es por ocurrencia, multiplicar
    const puntosDescontados = penalizacion.verificacion.por_ocurrencia
      ? penalizacion.puntos * ocurrencias
      : penalizacion.puntos;

    penalizacionTotal += Math.abs(puntosDescontados);

    penalizacionesAplicadas.push({
      id: penalizacion.id,
      nombre: penalizacion.nombre,
      puntos: puntosDescontados,
      severidad: penalizacion.verificacion.severidad || 'media',
      ocurrencias: ocurrencias,
      detalle: penalizacion.verificacion.descripcion
    });
  }
});

return {
  json: {
    penalizacionTotal: penalizacionTotal,
    penalizacionesAplicadas: penalizacionesAplicadas
  }
};
```

### 5. Function Node - Calculador de Puntaje Final

```javascript
// Obtener resultados de todos los nodos anteriores
const categorias = [
  $node["Evaluar Arquitectura"].json,
  $node["Evaluar Persistencia"].json,
  $node["Evaluar Recursos"].json,
  $node["Evaluar Validacion"].json,
  $node["Evaluar Funcionalidad"].json
];

const bonificaciones = $node["Evaluar Bonificaciones"].json;
const penalizaciones = $node["Evaluar Penalizaciones"].json;
const rubrica = $node["Cargar Rubrica"].json.rubrica;

// Calcular puntaje base (suma de categorías)
let puntajeBase = categorias.reduce((sum, cat) => sum + cat.puntajeObtenido, 0);

// Calcular puntaje final
let puntajeFinal = puntajeBase + bonificaciones.bonificacionTotal - penalizaciones.penalizacionTotal;
puntajeFinal = Math.max(0, Math.min(110, puntajeFinal)); // Limitar entre 0 y 110

// Determinar nota y resultado
let notaFinal = '0';
let resultado = 'Desaprobado';
let aprobado = false;

rubrica.escala_calificacion.forEach(escala => {
  if (puntajeFinal >= escala.rango[0] && puntajeFinal <= escala.rango[1]) {
    notaFinal = escala.nota.toString();
    resultado = escala.resultado;
    aprobado = escala.simbolo === '✅';
  }
});

// Recopilar todas las observaciones
let todasObservaciones = [];
categorias.forEach(cat => {
  todasObservaciones.push(`\n### ${cat.categoriaNombre} (${cat.puntajeObtenido}/${cat.puntajeMaximo})`);
  cat.observaciones.forEach(obs => todasObservaciones.push(obs));
});

// Generar recomendaciones
let recomendaciones = [];
if (puntajeFinal < 60) {
  recomendaciones.push('❌ El proyecto necesita mejoras significativas en múltiples áreas para aprobar');
}
if (puntajeFinal >= 60 && puntajeFinal < 80) {
  recomendaciones.push('⚠ Considerar mejorar la documentación y validaciones para obtener mejor calificación');
}
if (bonificaciones.bonificacionTotal < 5) {
  recomendaciones.push('💡 Agregar documentación Javadoc completa puede sumar hasta +3 puntos');
}
if (penalizaciones.penalizacionTotal > 0) {
  penalizaciones.penalizacionesAplicadas.forEach(pen => {
    if (pen.severidad === 'critica') {
      recomendaciones.push(`🔴 CRÍTICO: ${pen.nombre} debe ser corregido inmediatamente`);
    }
  });
}

// Formato de salida
return {
  json: {
    evaluacion_fecha: new Date().toISOString(),
    puntaje_total: puntajeFinal,
    puntaje_base: puntajeBase,
    bonificaciones_obtenidas: bonificaciones.bonificacionTotal,
    penalizaciones_aplicadas: penalizaciones.penalizacionTotal,
    nota_final: notaFinal,
    resultado: resultado,
    aprobado: aprobado,
    desglose_categorias: categorias.map(cat => ({
      id: cat.categoriaId,
      nombre: cat.categoriaNombre,
      puntaje_obtenido: cat.puntajeObtenido,
      puntaje_maximo: cat.puntajeMaximo,
      porcentaje: cat.porcentaje,
      observaciones: cat.observaciones
    })),
    bonificaciones_detalle: bonificaciones.bonificacionesObtenidas,
    penalizaciones_detalle: penalizaciones.penalizacionesAplicadas,
    observaciones: todasObservaciones,
    recomendaciones: recomendaciones,
    resumen: {
      fortalezas: todasObservaciones.filter(o => o.includes('✅')),
      areas_mejora: todasObservaciones.filter(o => o.includes('❌') || o.includes('⚠'))
    }
  }
};
```

---

## 📊 Formato de Salida

### JSON Completo

```json
{
  "evaluacion_fecha": "2025-01-15T10:30:00Z",
  "puntaje_total": 104,
  "puntaje_base": 97,
  "bonificaciones_obtenidas": 7,
  "penalizaciones_aplicadas": 0,
  "nota_final": "10",
  "resultado": "Excelente - Aprobado con Distinción",
  "aprobado": true,
  "desglose_categorias": [
    {
      "id": 1,
      "nombre": "Arquitectura y Diseño",
      "puntaje_obtenido": 30,
      "puntaje_maximo": 30,
      "porcentaje": "100.00",
      "observaciones": [
        "✅ Excelente: 5 paquetes detectados (Config, Models, Dao, Service, Main)",
        "✅ Patrón DAO implementado: 2 clases DAO",
        "✅ Queries SQL como constantes: 12 constantes"
      ]
    }
  ],
  "bonificaciones_detalle": [
    {
      "id": "B1",
      "nombre": "Documentación Javadoc Completa",
      "puntos": 3,
      "detalle": "35 ocurrencias detectadas (mínimo: 30)"
    }
  ],
  "observaciones": [
    "### Arquitectura y Diseño (30/30)",
    "✅ Excelente: 5 paquetes detectados",
    "✅ Patrón DAO implementado: 2 clases DAO"
  ],
  "recomendaciones": [
    "✅ Excelente trabajo. El proyecto cumple con todos los criterios de calidad"
  ],
  "resumen": {
    "fortalezas": [
      "✅ Excelente: 5 paquetes detectados",
      "✅ Patrón DAO implementado"
    ],
    "areas_mejora": []
  }
}
```

---

## 🚀 Pasos para Implementar en n8n

### Paso 1: Crear Workflow

1. Abrir n8n
2. Crear nuevo workflow: "Evaluador TPI Prog2"
3. Agregar nodo Webhook inicial

### Paso 2: Configurar Webhook

- **Method**: POST
- **Path**: `/evaluar-tpi`
- **Response Mode**: Last Node
- **Binary Property**: `data`

### Paso 3: Agregar Nodo "Read Binary File"

Para leer el archivo subido.

### Paso 4: Agregar Nodo "HTTP Request" o "Read File"

Para cargar `RUBRICA_N8N.json`:
- **Method**: GET
- **URL**: Ruta al archivo JSON de la rúbrica

### Paso 5: Agregar Function Nodes

Copiar el código proporcionado arriba para cada nodo:
1. Extractor de Clases
2. Evaluadores de Categorías (5 nodos)
3. Evaluador de Bonificaciones
4. Evaluador de Penalizaciones
5. Calculador de Puntaje Final

### Paso 6: Configurar Respuesta

Agregar nodo "Respond to Webhook" con el JSON generado.

### Paso 7: Activar Workflow

Hacer clic en "Active" para habilitar el endpoint.

---

## 🧪 Prueba del Sistema

### Usando curl

```bash
curl -X POST \
  http://localhost:5678/webhook/evaluar-tpi \
  -F "data=@proyecto_completo.txt" \
  -H "Content-Type: multipart/form-data"
```

### Usando Postman

1. Method: POST
2. URL: `http://localhost:5678/webhook/evaluar-tpi`
3. Body: form-data
4. Key: `data` (type: File)
5. Value: Seleccionar archivo `proyecto_completo.txt`

---

## 📧 Opcional: Enviar Resultados por Email

Agregar nodo "Send Email" después del calculador:

```javascript
// En un Function Node antes del email
const resultado = $input.item.json;

const htmlBody = `
<h1>Evaluación TPI - Programación 2</h1>
<h2>Resultado: ${resultado.resultado}</h2>
<p><strong>Puntaje Total:</strong> ${resultado.puntaje_total}/110</p>
<p><strong>Nota Final:</strong> ${resultado.nota_final}</p>
<p><strong>Estado:</strong> ${resultado.aprobado ? '✅ APROBADO' : '❌ DESAPROBADO'}</p>

<h3>Desglose por Categoría</h3>
<ul>
${resultado.desglose_categorias.map(cat =>
  `<li><strong>${cat.nombre}:</strong> ${cat.puntaje_obtenido}/${cat.puntaje_maximo} (${cat.porcentaje}%)</li>`
).join('')}
</ul>

<h3>Observaciones</h3>
<pre>${resultado.observaciones.join('\n')}</pre>

<h3>Recomendaciones</h3>
<ul>
${resultado.recomendaciones.map(rec => `<li>${rec}</li>`).join('')}
</ul>
`;

return {
  json: {
    to: 'estudiante@universidad.edu',
    subject: `Evaluación TPI - ${resultado.aprobado ? 'APROBADO' : 'DESAPROBADO'} - Nota: ${resultado.nota_final}`,
    html: htmlBody,
    attachments: [
      {
        filename: 'evaluacion_detallada.json',
        content: JSON.stringify(resultado, null, 2)
      }
    ]
  }
};
```

---

## ⚠️ Consideraciones Importantes

1. **Validación Manual**: Algunos criterios requieren verificación manual:
   - Compilación del código
   - Funcionalidad de operaciones CRUD
   - Conexión a base de datos

2. **Limitaciones del Análisis Estático**:
   - No detecta errores lógicos
   - No verifica funcionalidad real
   - No compila el código

3. **Recomendaciones**:
   - Usar este sistema como **evaluación preliminar**
   - Complementar con **revisión manual** para puntaje final
   - Verificar **criterios críticos** manualmente

4. **Seguridad**:
   - Validar tamaño máximo de archivo (ej: 5MB)
   - Sanitizar entrada para evitar inyección de código
   - Limitar tasa de requests (rate limiting)

---

## 📚 Archivos Relacionados

- `RUBRICA_N8N.json`: Definición completa de criterios de evaluación
- `RUBRICA_EVALUACION.md`: Rúbrica en formato legible para humanos
- `README.md`: Documentación del proyecto
- `CLAUDE.md`: Guía técnica

---

## 🆘 Soporte

Si encuentras problemas:
1. Verificar que n8n está actualizado (v0.200+)
2. Revisar logs de n8n: `docker logs n8n` o consola del workflow
3. Validar formato del archivo de entrada
4. Probar con archivo de ejemplo incluido en el proyecto

---

**Versión**: 1.0
**Última actualización**: 2025-01-15
**Compatibilidad**: n8n v0.200+
