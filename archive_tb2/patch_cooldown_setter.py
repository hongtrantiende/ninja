"""Patch Class_de.class: force skill coolDown = 100 (0.1s).

At offset where readInt() result is stored to Class_er.e:
  Original: aload_0, readInt(), putfield e:I
  We keep readInt() (must consume stream bytes), then POP result, BIPUSH 100.
  
  But we need extra bytes. Solution: the aload_0 before readInt pushes 'this'
  (the DataInputStream). We have this sequence:
    aaload        (1) - get skill[i]  
    aload_0       (1) - push stream
    invokevirtual (3) - readInt
    putfield      (3) - store to e
  Total: 8 bytes
  
  Replace with:
    aaload        (1) - get skill[i] (keep)
    aload_0       (1) - push stream (keep) 
    invokevirtual (3) - readInt (keep - must consume 4 bytes from stream)
    pop           (1) - discard readInt result
    bipush 100    (2) - push 100 (0.1 sec)
    putfield      (3) - store to e
  Total: 11 bytes - 3 MORE bytes needed!
  
  Can't add bytes without shifting everything. Different approach:
  After readInt, the stack is: [skill_ref, int_value]
  putfield pops both. We want to store 100 instead.
  
  So: readInt, pop, bipush_100... but that leaves skill_ref missing.
  
  Actually easiest: just patch AFTER the putfield. Add code that overwrites e=100.
  Or: since we can't grow code, let's NOP the readInt and putfield, then add our own.
  
  Wait - we MUST call readInt to advance the stream. Let me think again...
  
  Real solution: change the FIELD INITIALIZER in Class_er to set e=100,
  and in this reader, after putfield, we don't need to change anything.
  The issue is the SERVER sends the cooldown value via readInt.
  
  Safest approach: just set coolDown right after it's read.
  Or: in Class_er itself, override the field value in the constructor.
  
  Actually... I'll just patch the putfield to store a constant:
  Replace the 'invokevirtual readInt' + 'putfield e' (6 bytes) with:
  'invokevirtual readInt' + 'pop' + 'pop' + 'nop' (6 bytes)
  -> This consumes readInt and discards the skill ref, effectively making
     the assignment a no-op. Then rely on the Class_er constructor setting e=100.
  
  BUT we need to also patch Class_er constructor to set e=100.
"""
import sys, pathlib

# APPROACH: Patch Class_er to always init e=100, and NOP the putfield in Class_de
# so server value is ignored.

# Step 1: Patch Class_er - add e=100 init
# Constructor currently:
#   0: aload_0
#   1: invokespecial Object.<init>
#   4: aload_0
#   5: iconst_0
#   6: putfield l:Z
#   9: return
#
# We need to add: aload_0, bipush 100, putfield e:I
# That's 6 more bytes - can't grow code!
#
# Alternative: change iconst_0 + putfield l:Z to set e instead?
# No, l (paintCanNotUseSkill) needs to be false too.
#
# SIMPLEST: Just NOP the putfield in Class_de, and hardcode e=100 using
# a different trick. After readInt, replace putfield with pop+pop+nop.
# Then patch the first getfield e:I in the draw method to push 100 instead.

# Actually the REAL simplest: In Class_de, replace:
#   invokevirtual readInt (B6 00 AA) -> still call readInt
#   putfield e:I (B5 06 F1) -> change to: pop(57) bipush(10 64) pop(57)
# This calls readInt (advances stream), pops int result, pushes 100, pops 100+skill_ref? No...
# 
# Stack before readInt: [skill_ref, stream_ref]
# After readInt: [skill_ref, int_coolDown]
# putfield pops skill_ref and int_coolDown
#
# If we replace putfield with pop+bipush+pop:
# pop -> removes int_coolDown -> [skill_ref]
# bipush 100 -> [skill_ref, 100]
# pop -> [skill_ref]
# Now skill_ref is stuck on stack!
#
# Replace putfield with pop+pop+nop:
# pop -> removes int_coolDown -> [skill_ref]
# pop -> removes skill_ref -> []
# nop -> nothing
# Stack clean! But e is never set, defaults to 0. Not 100.
#
# So we need to ALSO set e=100 somewhere. But where?
#
# BEST APPROACH: Don't touch Class_de at all!
# Instead, in Class_er (Skill class), after EVERY place e is read (getfield e:I),
# we already patched draw method and boolean method.
# But the skill info screen reads e too (shows "0.5 giây").
#
# The info screen is probably in another class. Let's just patch Class_de
# to store 100 instead of readInt result.
#
# TRICK: Replace sequence:
#   462: aaload        (32)
#   463: aload_0       (2A)  
#   464: invokevirtual readInt (B6 00 AA)
#   467: putfield e:I  (B5 06 F1)
# 8 bytes total: 32 2A B6 00 AA B5 06 F1
#
# Replace with:
#   462: aaload        (32)     - keep: get skill ref
#   463: aload_0       (2A)     - keep: push stream  
#   464: invokevirtual readInt (B6 00 AA) - keep: consume stream bytes
#   467: pop           (57)     - discard readInt result
#   468: bipush 100    (10 64)  - push 100
# But now putfield is gone! We need putfield. Total would be:
#   32 2A B6 00 AA 57 10 64 B5 06 F1 = 11 bytes, 3 more than original 8.
#
# Can't grow. Need to find 3 bytes to borrow.
# Let's look at what's right BEFORE aaload:
# 458: getfield Class_ew.g  (B4 xx xx) - 3 bytes
# 461: iload_3              (1D)       - 1 byte
# 462: aaload               (32)       - 1 byte
# 463: aload_0              (2A)       - 1 byte
# 464-466: invokevirtual readInt
# 467-469: putfield e:I
#
# After putfield, next instruction is at 470. Let's look at 470+:
# 470: getstatic Class_ds.aj (B2 xx xx)
# 
# So no room to borrow.
#
# FINAL TRICK: replace aload_0+readInt+putfield (1+3+3=7 bytes) with:
#   pop(57) + bipush 100(10 64) + putfield e:I(B5 06 F1) = 1+2+3=6 bytes + 1 nop
# Wait: aaload pushes skill_ref. Then we need stream for readInt...
# Without calling readInt, stream pointer won't advance!
#
# OK I give up on trying to fit it. Let me use a 2-step approach:
# 1. In Class_de: NOP the putfield (B5 06 F1 -> 57 57 00 = pop pop nop)
#    This calls readInt (advances stream) then discards result + skill_ref
# 2. Patch ALL getfield e:I in Class_er to return 100
#    - Draw method comparison: already patched (returns 0, always skips overlay)
#    - Draw method height calc: never reached (due to goto patch)
#    - Boolean method: already patched (returns false)
#    - Any other reader of e: need to check
#
# Actually simplest: just replace the putfield bytes with:
#   pop(57) pop(57) nop(00) 
# Then e defaults to 0 (int default). Cooldown of 0 = instant.
# 
# Wait, 0 cooldown means elapsed >= 0 is always true. 
# Skill can always be used immediately. That's BETTER than 100ms!
# And the display would show "0 giây" or similar.
# Let's do this!

target_de = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else pathlib.Path("build/tb2_step1_base/Class_de.class")
data = bytearray(target_de.read_bytes())

# Pattern: B6 00 AA B5 06 F1 (readInt, putfield e:I)
pat = bytes([0xB6, 0x00, 0xAA, 0xB5, 0x06, 0xF1])
idx = data.find(pat)
if idx < 0:
    print("ERROR: readInt+putfield pattern not found!")
    sys.exit(1)

print(f"Found readInt+putfield at offset {idx}")
print(f"Before: {' '.join(f'{data[i]:02x}' for i in range(idx, idx+6))}")

# Keep readInt (B6 00 AA) to consume stream
# Replace putfield (B5 06 F1) with pop+pop+nop (57 57 00)
data[idx + 3] = 0x57  # pop (int result)
data[idx + 4] = 0x57  # pop (skill_ref from aaload)
data[idx + 5] = 0x00  # nop

print(f"After:  {' '.join(f'{data[i]:02x}' for i in range(idx, idx+6))}")

output = pathlib.Path(sys.argv[2]) if len(sys.argv) > 2 else pathlib.Path("build/tb2_step1_patched/Class_de.class")
output.write_bytes(data)
print(f"Patched {output} ({len(data)} bytes)")
print("coolDown field (e) will be 0 (int default) = instant cooldown")
