const fs = require('fs');
const src = 'D:\\Users\\Downloads\\develop\\slidecleaner\\app\\src\\main\\java\\com\\gallery\\cleaner\\ui\\screen\\media\\MediaSwipeScreen.kt';
let lines = fs.readFileSync(src, 'utf-8').split('\n');

// Remove the top bar delete button block (lines 281-286, 1-indexed)
// Line 281: if (uiState.deleteQueue.items.isNotEmpty()) {
// Line 282: IconButton(onClick = { viewModel.showDeleteConfirmDialog() }) {
// Line 283: Icon(Icons.Default.CheckCircle, contentDescription = "确认删除", tint = AppColors.Destructive)
// Line 284: }
// Line 285: }
// Line 286: },
const before = lines.slice(0, 280);  // lines 1-280
const after = lines.slice(285);     // lines 286+ (0-indexed 285 = line 286)

const newLines = [...before, ...after];
fs.writeFileSync(src, newLines.join('\n'), 'utf-8');
console.log('Removed top bar delete button');
console.log('Before line count:', lines.length);
console.log('After line count:', newLines.length);