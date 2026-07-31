package com.gratitudelogger.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

const val PIN_LENGTH = 4

private val padRows = listOf(
    listOf('1', '2', '3'),
    listOf('4', '5', '6'),
    listOf('7', '8', '9'),
    listOf(null, '0', '⌫')
)

@Composable
fun PinPad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        padRows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    when (key) {
                        null -> Box(modifier = Modifier.size(72.dp))
                        '⌫' -> TextButton(onClick = onBackspace, modifier = Modifier.size(72.dp)) {
                            Text("⌫", style = MaterialTheme.typography.headlineSmall)
                        }
                        else -> TextButton(onClick = { onDigit(key) }, modifier = Modifier.size(72.dp)) {
                            Text(key.toString(), style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinDotsIndicator(length: Int, maxLength: Int = PIN_LENGTH, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center) {
        repeat(maxLength) { index ->
            val filled = index < length
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .size(14.dp)
                    .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    .then(
                        if (filled) {
                            Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}
