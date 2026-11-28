' Generador de códigos QR (modo byte, corrección M, versiones 1 a 10).
' Basado en el algoritmo de referencia de Project Nayuki. Solo usa aritmética entera
' para que funcione igual en cualquier Roku.

function qrMatrix(text as string) as object
    ba = CreateObject("roByteArray")
    ba.FromAsciiString(text)
    data = []
    for i = 0 to ba.Count() - 1
        data.Push(ba[i])
    end for
    eccPer = [-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26]
    blocksN = [-1, 1, 1, 1, 2, 2, 4, 4, 4, 5, 5]
    ver = 0
    for v = 1 to 10
        cap = qrDataCodewords(v, eccPer, blocksN) * 8
        ccBits = 8
        if v >= 10 then ccBits = 16
        if 4 + ccBits + data.Count() * 8 <= cap then
            ver = v
            exit for
        end if
    end for
    if ver = 0 then return invalid
    ccBits = 8
    if ver >= 10 then ccBits = 16

    bits = []
    qrAppend(bits, 4, 4)
    qrAppend(bits, data.Count(), ccBits)
    for each b in data
        qrAppend(bits, b, 8)
    end for
    capBits = qrDataCodewords(ver, eccPer, blocksN) * 8
    term = capBits - bits.Count()
    if term > 4 then term = 4
    qrAppend(bits, 0, term)
    while bits.Count() mod 8 <> 0
        bits.Push(0)
    end while
    pad = 236
    while bits.Count() < capBits
        qrAppend(bits, pad, 8)
        if pad = 236 then pad = 17 else pad = 236
    end while
    codewords = []
    for i = 0 to bits.Count() - 1 step 8
        v8 = 0
        for j = 0 to 7
            v8 = v8 * 2 + bits[i + j]
        end for
        codewords.Push(v8)
    end for

    allCw = qrAddEcc(codewords, ver, eccPer[ver], blocksN[ver])
    size = ver * 4 + 17
    q = {size: size, mods: [], fn: []}
    for y = 0 to size - 1
        row = []
        frow = []
        for x = 0 to size - 1
            row.Push(false)
            frow.Push(false)
        end for
        q.mods.Push(row)
        q.fn.Push(frow)
    end for
    qrFunctionPatterns(q, ver)
    qrDrawCodewords(q, allCw)

    best = 0
    bestPen = 2147483647
    for mask = 0 to 7
        qrApplyMask(q, mask)
        qrFormatBits(q, mask)
        pen = qrPenalty(q)
        if pen < bestPen then
            bestPen = pen
            best = mask
        end if
        qrApplyMask(q, mask)
    end for
    qrApplyMask(q, best)
    qrFormatBits(q, best)
    return q.mods
end function

function qrPow2(n as integer) as integer
    r = 1
    for i = 1 to n
        r = r * 2
    end for
    return r
end function

function qrBit(v as integer, i as integer) as integer
    return (v \ qrPow2(i)) mod 2
end function

function qrXor(a as integer, b as integer) as integer
    r = 0
    p = 1
    x = a
    y = b
    while x > 0 or y > 0
        if (x mod 2) <> (y mod 2) then r = r + p
        x = x \ 2
        y = y \ 2
        p = p * 2
    end while
    return r
end function

sub qrAppend(bits as object, value as integer, n as integer)
    for i = n - 1 to 0 step -1
        bits.Push(qrBit(value, i))
    end for
end sub

function qrRawModules(ver as integer) as integer
    r = (16 * ver + 128) * ver + 64
    if ver >= 2 then
        na = ver \ 7 + 2
        r = r - ((25 * na - 10) * na - 55)
        if ver >= 7 then r = r - 36
    end if
    return r
end function

function qrDataCodewords(ver as integer, eccPer as object, blocksN as object) as integer
    return qrRawModules(ver) \ 8 - eccPer[ver] * blocksN[ver]
end function

function qrMul(x as integer, y as integer) as integer
    z = 0
    for i = 7 to 0 step -1
        z = z * 2
        if z >= 256 then z = qrXor(z, 285)
        if qrBit(y, i) = 1 then z = qrXor(z, x)
    end for
    return z
end function

function qrDivisor(degree as integer) as object
    res = []
    for i = 1 to degree - 1
        res.Push(0)
    end for
    res.Push(1)
    root = 1
    for i = 0 to degree - 1
        for j = 0 to degree - 1
            res[j] = qrMul(res[j], root)
            if j + 1 < degree then res[j] = qrXor(res[j], res[j + 1])
        end for
        root = qrMul(root, 2)
    end for
    return res
end function

function qrRemainder(data as object, div as object) as object
    res = []
    for i = 0 to div.Count() - 1
        res.Push(0)
    end for
    for each b in data
        factor = qrXor(b, res[0])
        res.Shift()
        res.Push(0)
        for i = 0 to div.Count() - 1
            res[i] = qrXor(res[i], qrMul(div[i], factor))
        end for
    end for
    return res
end function

function qrAddEcc(data as object, ver as integer, eccLen as integer, numBlocks as integer) as object
    raw = qrRawModules(ver) \ 8
    numShort = numBlocks - (raw mod numBlocks)
    shortLen = raw \ numBlocks
    div = qrDivisor(eccLen)
    blocks = []
    k = 0
    for i = 0 to numBlocks - 1
        n = shortLen - eccLen
        if i >= numShort then n = n + 1
        dat = []
        for j = 0 to n - 1
            dat.Push(data[k + j])
        end for
        k = k + n
        ecc = qrRemainder(dat, div)
        if i < numShort then dat.Push(0)
        for each e in ecc
            dat.Push(e)
        end for
        blocks.Push(dat)
    end for
    out = []
    for i = 0 to blocks[0].Count() - 1
        for j = 0 to blocks.Count() - 1
            if i <> shortLen - eccLen or j >= numShort then out.Push(blocks[j][i])
        end for
    end for
    return out
end function

sub qrSet(q as object, x as integer, y as integer, dark as boolean)
    q.mods[y][x] = dark
    q.fn[y][x] = true
end sub

sub qrFunctionPatterns(q as object, ver as integer)
    size = q.size
    for i = 0 to size - 1
        qrSet(q, 6, i, (i mod 2) = 0)
        qrSet(q, i, 6, (i mod 2) = 0)
    end for
    for each c in [[3, 3], [size - 4, 3], [3, size - 4]]
        for dy = -4 to 4
            for dx = -4 to 4
                xx = c[0] + dx
                yy = c[1] + dy
                if xx >= 0 and xx < size and yy >= 0 and yy < size then
                    d = Abs(dx)
                    if Abs(dy) > d then d = Abs(dy)
                    qrSet(q, xx, yy, d <> 2 and d <> 4)
                end if
            end for
        end for
    end for
    apos = qrAlignPositions(ver, size)
    n = apos.Count()
    for i = 0 to n - 1
        for j = 0 to n - 1
            corner = (i = 0 and j = 0) or (i = 0 and j = n - 1) or (i = n - 1 and j = 0)
            if not corner then
                for dy = -2 to 2
                    for dx = -2 to 2
                        d = Abs(dx)
                        if Abs(dy) > d then d = Abs(dy)
                        qrSet(q, apos[i] + dx, apos[j] + dy, d <> 1)
                    end for
                end for
            end if
        end for
    end for
    qrFormatBits(q, 0)
    if ver >= 7 then
        r = ver
        for i = 1 to 12
            r = r * 2
            if r >= 4096 then r = qrXor(r, 7973)
        end for
        bitsV = ver * 4096 + r
        for i = 0 to 17
            bit = qrBit(bitsV, i) = 1
            a = size - 11 + (i mod 3)
            b = i \ 3
            qrSet(q, a, b, bit)
            qrSet(q, b, a, bit)
        end for
    end if
end sub

function qrAlignPositions(ver as integer, size as integer) as object
    if ver = 1 then return []
    na = ver \ 7 + 2
    stepv = ((ver * 8 + na * 3 + 5) \ (na * 4 - 4)) * 2
    res = []
    p = size - 7
    while res.Count() < na - 1
        res.Unshift(p)
        p = p - stepv
    end while
    res.Unshift(6)
    return res
end function

sub qrFormatBits(q as object, mask as integer)
    size = q.size
    data = 0 * 8 + mask
    r = data
    for i = 1 to 10
        r = r * 2
        if r >= 1024 then r = qrXor(r, 1335)
    end for
    bits = qrXor(data * 1024 + r, 21522)
    for i = 0 to 5
        qrSet(q, 8, i, qrBit(bits, i) = 1)
    end for
    qrSet(q, 8, 7, qrBit(bits, 6) = 1)
    qrSet(q, 8, 8, qrBit(bits, 7) = 1)
    qrSet(q, 7, 8, qrBit(bits, 8) = 1)
    for i = 9 to 14
        qrSet(q, 14 - i, 8, qrBit(bits, i) = 1)
    end for
    for i = 0 to 7
        qrSet(q, size - 1 - i, 8, qrBit(bits, i) = 1)
    end for
    for i = 8 to 14
        qrSet(q, 8, size - 15 + i, qrBit(bits, i) = 1)
    end for
    qrSet(q, 8, size - 8, true)
end sub

sub qrDrawCodewords(q as object, data as object)
    size = q.size
    i = 0
    total = data.Count() * 8
    right = size - 1
    while right >= 1
        if right = 6 then right = 5
        for vert = 0 to size - 1
            for j = 0 to 1
                x = right - j
                upward = ((right + 1) \ 2) mod 2 = 0
                if upward then y = size - 1 - vert else y = vert
                if not q.fn[y][x] and i < total then
                    q.mods[y][x] = qrBit(data[i \ 8], 7 - (i mod 8)) = 1
                    i = i + 1
                end if
            end for
        end for
        right = right - 2
    end while
end sub

sub qrApplyMask(q as object, mask as integer)
    size = q.size
    for y = 0 to size - 1
        for x = 0 to size - 1
            if not q.fn[y][x] then
                if mask = 0 then
                    inv = (x + y) mod 2 = 0
                else if mask = 1 then
                    inv = y mod 2 = 0
                else if mask = 2 then
                    inv = x mod 3 = 0
                else if mask = 3 then
                    inv = (x + y) mod 3 = 0
                else if mask = 4 then
                    inv = (x \ 3 + y \ 2) mod 2 = 0
                else if mask = 5 then
                    inv = (x * y) mod 2 + (x * y) mod 3 = 0
                else if mask = 6 then
                    inv = ((x * y) mod 2 + (x * y) mod 3) mod 2 = 0
                else
                    inv = ((x + y) mod 2 + (x * y) mod 3) mod 2 = 0
                end if
                if inv then q.mods[y][x] = not q.mods[y][x]
            end if
        end for
    end for
end sub

' Penalización simplificada (rachas, bloques 2x2 y balance) para elegir la mejor máscara.
function qrPenalty(q as object) as integer
    size = q.size
    mm = q.mods
    pen = 0
    dark = 0
    for y = 0 to size - 1
        runC = mm[y][0]
        runX = 1
        runY = 1
        colC = mm[0][y]
        for x = 0 to size - 1
            if mm[y][x] then dark = dark + 1
            if x > 0 then
                if mm[y][x] = runC then
                    runX = runX + 1
                    if runX = 5 then
                        pen = pen + 3
                    else if runX > 5 then
                        pen = pen + 1
                    end if
                else
                    runC = mm[y][x]
                    runX = 1
                end if
                if mm[x][y] = colC then
                    runY = runY + 1
                    if runY = 5 then
                        pen = pen + 3
                    else if runY > 5 then
                        pen = pen + 1
                    end if
                else
                    colC = mm[x][y]
                    runY = 1
                end if
            end if
            if x < size - 1 and y < size - 1 then
                c = mm[y][x]
                if c = mm[y][x + 1] and c = mm[y + 1][x] and c = mm[y + 1][x + 1] then pen = pen + 3
            end if
        end for
    end for
    total = size * size
    k = (Abs(dark * 20 - total * 10) + total - 1) \ total - 1
    if k > 0 then pen = pen + k * 10
    return pen
end function
