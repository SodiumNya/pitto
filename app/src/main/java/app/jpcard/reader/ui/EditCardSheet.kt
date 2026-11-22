package app.jpcard.reader.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.jpcard.reader.R
import app.jpcard.reader.data.CardFaces
import app.jpcard.reader.data.SavedCard

/** Edit sheet: name and card face (pick a photo / reset). The name is saved when the sheet closes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCardSheet(card: SavedCard, state: MainViewModel, actions: AppActions, onDismiss: () -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(card.name) }
    val face = rememberCardFace(card.idm, state.faceVersion)
    // System photo picker: needs no storage permission.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) actions.pickFace(card.idm, uri)
    }
    val close = {
        if (name.trim() != card.name) actions.rename(card.idm, name.trim())
        onDismiss()
    }

    ModalBottomSheet(onDismissRequest = close, sheetState = sheet) {
        Column(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.edit_card), style = MaterialTheme.typography.titleLarge)

            CardFaceBox(
                type = card.data.type,
                face = face,
                shape = RoundedCornerShape(14.dp),
                scrim = false,
                modifier = Modifier.fillMaxWidth(0.62f).aspectRatio(CardFaces.RATIO).align(Alignment.CenterHorizontally),
            ) {
                if (face == null) {
                    Text(
                        card.data.type.displayName(LocalContext.current),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                if (state.faceBusy) {
                    CircularProgressIndicator(Modifier.size(28.dp).align(Alignment.Center), color = Color.White)
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            ) {
                FilledTonalButton(
                    enabled = !state.faceBusy,
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                ) { Text(stringResource(if (face == null) R.string.choose_face else R.string.change_face)) }
                if (face != null) {
                    TextButton(enabled = !state.faceBusy, onClick = { actions.clearFace(card.idm) }) {
                        Text(stringResource(R.string.restore_default))
                    }
                }
            }
            Text(
                stringResource(R.string.face_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(20) },
                label = { Text(stringResource(R.string.name)) },
                placeholder = { Text(card.data.type.displayName(LocalContext.current)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))
            Button(onClick = close, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.done)) }
        }
    }
}
