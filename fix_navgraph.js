const fs = require('fs');
const src = 'D:\\Users\\Downloads\\develop\\slidecleaner\\app\\src\\main\\java\\com\\gallery\\cleaner\\ui\\navigation\\NavGraph.kt';
let c = fs.readFileSync(src, 'utf-8');

// Replace all forward/backward transition references with shared names
c = c.replace(/forwardEnterTransition/g, 'sharedEnterTransition');
c = c.replace(/forwardExitTransition/g, 'sharedExitTransition');
c = c.replace(/backwardEnterTransition/g, 'sharedEnterTransition');
c = c.replace(/backwardExitTransition/g, 'sharedExitTransition');

// Now collapse the 4 definitions into 2
// Find and remove duplicate sharedEnterTransition definitions (keep first)
const lines = c.split('\n');
const result = [];
let enterFound = false, exitFound = false, inTransitionBlock = false;
let skipUntilNextDef = false;
let braceDepth = 0;

for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    const trimmed = line.trim();
    
    if (trimmed.startsWith('val sharedEnterTransition') || trimmed.startsWith('val sharedExitTransition')) {
        if (trimmed.includes('sharedEnterTransition') && enterFound) {
            // Skip this duplicate definition
            inTransitionBlock = true;
            braceDepth = 0;
            for (let j = i; j < lines.length; j++) {
                for (const ch of lines[j]) {
                    if (ch === '(' || ch === '{') braceDepth++;
                    if (ch === ')' || ch === '}') braceDepth--;
                }
                if (braceDepth <= 0 && j > i) {
                    i = j;
                    break;
                }
            }
            continue;
        }
        if (trimmed.includes('sharedExitTransition') && exitFound) {
            inTransitionBlock = true;
            braceDepth = 0;
            for (let j = i; j < lines.length; j++) {
                for (const ch of lines[j]) {
                    if (ch === '(' || ch === '{') braceDepth++;
                    if (ch === ')' || ch === '}') braceDepth--;
                }
                if (braceDepth <= 0 && j > i) {
                    i = j;
                    break;
                }
            }
            continue;
        }
        if (trimmed.includes('sharedEnterTransition')) enterFound = true;
        if (trimmed.includes('sharedExitTransition')) exitFound = true;
    }
    
    result.push(line);
}

fs.writeFileSync(src, result.join('\n'), 'utf-8');
console.log('Done');