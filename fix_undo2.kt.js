const fs = require('fs');
const path = 'D:\\Users\\Downloads\\develop\\slidecleaner\\app\\src\\main\\java\\com\\gallery\\cleaner\\ui\\screen\\media\\MediaSwipeScreen.kt';
let c = fs.readFileSync(path, 'utf-8');

// Add onClick parameter to SwipeStatusChip UNDO call
c = c.replace(
    '                SwipeStatusChip(\n                    label = if (canUndo) "UNDO ON" else "UNDO",\n                    tint = if (canUndo) AppColors.Primary else AppColors.TextDisabled\n                )',
    '                SwipeStatusChip(\n                    label = if (canUndo) "UNDO ON" else "UNDO",\n                    tint = if (canUndo) AppColors.Primary else AppColors.TextDisabled,\n                    onClick = onUndo\n                )'
);

// Add onClick parameter to SwipeStatusChip REDO call
c = c.replace(
    '                SwipeStatusChip(\n                    label = if (canRedo) "REDO ON" else "REDO",\n                    tint = if (canRedo) AppColors.Accent else AppColors.TextDisabled\n                )',
    '                SwipeStatusChip(\n                    label = if (canRedo) "REDO ON" else "REDO",\n                    tint = if (canRedo) AppColors.Accent else AppColors.TextDisabled,\n                    onClick = onRedo\n                )'
);

// Add onClick parameter to SwipeStatusChip function signature
c = c.replace(
    'private fun SwipeStatusChip(\n    label: String,\n    tint: Color\n)',
    'private fun SwipeStatusChip(\n    label: String,\n    tint: Color,\n    onClick: (() -> Unit)? = null\n)'
);

// Add clickable modifier to SwipeStatusChip Box - find the Box and add .clickable
c = c.replace(
    '    Box(\n        modifier = Modifier\n            .border(1.dp, tint.copy(alpha = 0.35f), AppShape.Pill)\n            .background(tint.copy(alpha = 0.12f), AppShape.Pill)\n            .padding(horizontal = AppPadding.SM, vertical = AppPadding.XS)\n    )',
    '    Box(\n        modifier = Modifier\n            .then(if (onClick != null) Modifier.clickable { onClick.invoke() } else Modifier)\n            .border(1.dp, tint.copy(alpha = 0.35f), AppShape.Pill)\n            .background(tint.copy(alpha = 0.12f), AppShape.Pill)\n            .padding(horizontal = AppPadding.SM, vertical = AppPadding.XS)\n    )'
);

fs.writeFileSync(path, c, 'utf-8');
console.log('Done');