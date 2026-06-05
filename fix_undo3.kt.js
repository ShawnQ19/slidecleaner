const fs = require('fs');
const path = 'D:\\Users\\Downloads\\develop\\slidecleaner\\app\\src\\main\\java\\com\\gallery\\cleaner\\ui\\screen\\media\\MediaSwipeScreen.kt';
let c = fs.readFileSync(path, 'utf-8');

const nl = '\r\n';

// Add onClick parameter to SwipeStatusChip UNDO call
c = c.replace(
    `                SwipeStatusChip(${nl}                    label = if (canUndo) "UNDO ON" else "UNDO",${nl}                    tint = if (canUndo) AppColors.Primary else AppColors.TextDisabled${nl}                )`,
    `                SwipeStatusChip(${nl}                    label = if (canUndo) "UNDO ON" else "UNDO",${nl}                    tint = if (canUndo) AppColors.Primary else AppColors.TextDisabled,${nl}                    onClick = onUndo${nl}                )`
);

// Add onClick parameter to SwipeStatusChip REDO call
c = c.replace(
    `                SwipeStatusChip(${nl}                    label = if (canRedo) "REDO ON" else "REDO",${nl}                    tint = if (canRedo) AppColors.Accent else AppColors.TextDisabled${nl}                )`,
    `                SwipeStatusChip(${nl}                    label = if (canRedo) "REDO ON" else "REDO",${nl}                    tint = if (canRedo) AppColors.Accent else AppColors.TextDisabled,${nl}                    onClick = onRedo${nl}                )`
);

// Add onClick parameter to SwipeStatusChip function signature
c = c.replace(
    `private fun SwipeStatusChip(${nl}    label: String,${nl}    tint: Color${nl})`,
    `private fun SwipeStatusChip(${nl}    label: String,${nl}    tint: Color,${nl}    onClick: (() -> Unit)? = null${nl})`
);

// Add clickable modifier to SwipeStatusChip Box
c = c.replace(
    `    Box(${nl}        modifier = Modifier${nl}            .border(1.dp, tint.copy(alpha = 0.35f), AppShape.Pill)${nl}            .background(tint.copy(alpha = 0.12f), AppShape.Pill)${nl}            .padding(horizontal = AppPadding.SM, vertical = AppPadding.XS)${nl}    )`,
    `    Box(${nl}        modifier = Modifier${nl}            .then(if (onClick != null) Modifier.clickable { onClick.invoke() } else Modifier)${nl}            .border(1.dp, tint.copy(alpha = 0.35f), AppShape.Pill)${nl}            .background(tint.copy(alpha = 0.12f), AppShape.Pill)${nl}            .padding(horizontal = AppPadding.SM, vertical = AppPadding.XS)${nl}    )`
);

fs.writeFileSync(path, c, 'utf-8');
console.log('Done');