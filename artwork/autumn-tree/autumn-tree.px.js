pal({ b: "#d4d4d4", k: "#30211e", d: "#4a2b23", t: "#70412d", l: "#a76036", g: "#f5c52c", y: "#ffe66a", a: "#ef9229", r: "#d4512d", q: "#9b3927", s: "#bf662c" })

// Reference-matched pixel sprite. PXL downsamples the standalone reference to 128px
// and maps every pixel into this compact, hard-edged autumn palette.
scene("autumn_tree", { w: 128, h: 128, bg: "b" },
  image("autumn-tree-reference.png", 0, 0, {
    w: 128,
    h: 128,
    palette: "bkdltgyarqs",
    dither: 0
  }))
