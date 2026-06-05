const fs = require('fs');
const path = 'D:\\Users\\Downloads\\develop\\slidecleaner\\app\\src\\main\\java\\com\\gallery\\cleaner\\ui\\screen\\media\\MediaSwipeScreen.kt';
let c = fs.readFileSync(path, 'utf-8');

// 1. Add onUndo and onRedo params to SwipeStatusStrip call
c = c.replace(
    '        SwipeStatusStrip(\n            currentIndex = uiState.currentIndex + 1,\n            totalCount = uiState.mediaItems.size,\n            queuedCount = uiState.deleteQueue.items.size,\n            canUndo = canUndo,\n            canRedo = canRedo\n        )',
    '        SwipeStatusStrip(\n            currentIndex = uiState.currentIndex + 1,\n            totalCount = uiState.mediaItems.size,\n            queuedCount = uiState.deleteQueue.items.size,\n            canUndo = canUndo,\n            canRedo = canRedo,\n            onUndo = { viewModel.undo() },\n            onRedo = { viewModel.redo() }\n        )'
);

// 2. Add onUndo and onRedo params to SwipeStatusStrip function signature
c = c.replace(
    'private fun SwipeStatusStrip(\n    currentIndex: Int,\n    totalCount: Int,\n    queuedCount: Int,\n    canUndo: Boolean,\n    canRedo: Boolean\n)',
    'private fun SwipeStatusStrip(\n    currentIndex: Int,\n    totalCount: Int,\n    queuedCount: Int,\n    canUndo: Boolean,\n    canRedo: Boolean,\n    onUndo: (() -> Unit)? = null,\n    onRedo: (() -> Unit)? = null\n)'
);

// 3. Replace SwipeStatusChip for UNDO to be clickable
c = c.replace(
    '                SwipeStatusChip(\n                    label = if (canUndo) "UNDO ON" else "UNDO",\n                    tint = if (canUndo) AppColors.Primary else AppColors.TextDisabled\n                )',
    '                SwipeStatusChip(\n                    label = if (canUndo) "UNDO ON" else "UNDO",\n                    tint = if (canUndo) AppColors.Primary else AppColors.TextDisabled,\n                    onClick = onUndo\n                )'
);

// 4. Replace SwipeStatusChip for REDO to be clickable
c = c.replace(
    '                SwipeStatusChip(\n                    label = if (canRedo) "REDO ON" else "REDO",\n                    tint = if (canRedo) AppColors.Accent else AppColors.TextDisabled\n                )',
    '                SwipeStatusChip(\n                    label = if (canRedo) "REDO ON" else "REDO",\n                    tint = if (canRedo) AppColors.Accent else AppColors.TextDisabled,\n                    onClick = onRedo\n                )'
);

// 5. Add onClick param to SwipeStatusChip function
c = c.replace(
    'private fun SwipeStatusChip(\n    label: String,\n    tint: Color\n)',
    'private fun SwipeStatusChip(\n    label: String,\n    tint: Color,\n    onClick: (() -> Unit)? = null\n)'
);

// 6. Add clickable modifier to SwipeStatusChip Box
c = c.replace(
    '    Box(\n        modifier = Modifier\n            .border(1.dp, tint.copy(alpha = 0.35f), AppShape.Pill)\n            .background(tint.copy(alpha = 0.12f), AppShape.Pill)\n            .padding(horizontal = AppPadding.SM, vertical = AppPadding.XS)\n    ) {\n        Text(\n            text = label,',
    '    Box(\n        modifier = Modifier\n            .then(\n                if (onClick != null) {\n                    Modifier.pointerInput(Unit) {\n                        awaitEachGesture {\n                            val down = awaitFirstDown(requireUnconsumed = false)\n                            down.consume()\n                            onClick.invoke()\n                        }\n                    }\n                } else Modifier\n            )\n            .border(1.dp, tint.copy(alpha = 0.35f), AppShape.Pill)\n            .background(tint.copy(alpha = 0.12f), AppShape.Pill)\n            .padding(horizontal = AppPadding.SM, vertical = AppPadding.XS)\n    ) {\n        Text(\n            text = label,'
);

fs.writeFileSync(path, c, 'utf-8');
console.log('Done');