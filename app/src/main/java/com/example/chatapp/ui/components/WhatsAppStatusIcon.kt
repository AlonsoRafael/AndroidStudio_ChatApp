package com.example.chatapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

@Composable
fun WhatsAppStatusIcon(
    status: String,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.size(16.dp)
    ) {
        when (status) {
            "SENDING" -> drawClock(Color.Gray)
            "SENT" -> drawSingleCheck(Color.Gray)
            "DELIVERED" -> drawDoubleCheck(Color.Gray)
            "READ" -> drawDoubleCheck(Color.Blue)
            else -> drawSingleCheck(Color.Gray)
        }
    }
}

private fun DrawScope.drawClock(color: Color) {
    val center = Offset(size.width / 2, size.height / 2)
    val radius = size.width / 3
    
    // Desenhar círculo
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
    )
    
    // Desenhar ponteiros do relógio
    drawLine(
        color = color,
        start = center,
        end = Offset(center.x, center.y - radius * 0.6f),
        strokeWidth = 1.dp.toPx()
    )
    drawLine(
        color = color,
        start = center,
        end = Offset(center.x + radius * 0.4f, center.y),
        strokeWidth = 1.dp.toPx()
    )
}

private fun DrawScope.drawSingleCheck(color: Color) {
    val path = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.5f)
        lineTo(size.width * 0.4f, size.height * 0.7f)
        lineTo(size.width * 0.8f, size.height * 0.3f)
    }
    
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
    )
}

private fun DrawScope.drawDoubleCheck(color: Color) {
    // Primeira seta (mais à direita)
    val path1 = Path().apply {
        moveTo(size.width * 0.4f, size.height * 0.5f)
        lineTo(size.width * 0.6f, size.height * 0.7f)
        lineTo(size.width * 0.9f, size.height * 0.3f)
    }
    
    // Segunda seta (mais à esquerda, sobreposta)
    val path2 = Path().apply {
        moveTo(size.width * 0.1f, size.height * 0.5f)
        lineTo(size.width * 0.3f, size.height * 0.7f)
        lineTo(size.width * 0.6f, size.height * 0.3f)
    }
    
    drawPath(
        path = path1,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
    )
    
    drawPath(
        path = path2,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
    )
}
