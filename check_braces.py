
def check_braces(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    stack = []
    for i, line in enumerate(lines):
        for j, char in enumerate(line):
            if char == '{':
                stack.append((i + 1, j + 1))
            elif char == '}':
                if not stack:
                    print(f"Extra closing brace at line {i + 1}, col {j + 1}")
                    return
                stack.pop()
    
    if stack:
        print(f"Unclosed braces: {len(stack)}")
        print(f"Last unclosed brace at line {stack[-1][0]}, col {stack[-1][1]}")
    else:
        print("Braces are balanced.")

check_braces(r'd:\MyTurretMod\src\main\java\com\example\examplemod\SkeletonTurret.java')
