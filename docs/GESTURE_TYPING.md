# Glide & Gesture Typing Engine

The iOS Keyboard for Android includes a continuous-touch glide typing engine designed for fast one-handed input.

---

## 1. Visual Trail Pipeline

As the user glides their finger across the keyboard:
1. `MotionEvent.ACTION_MOVE` registers screen coordinates $(x_i, y_i, t_i)$.
2. Old points ($>250$ms) are automatically pruned to create a trailing fade effect.
3. Points are rendered as a smooth quadratic bezier curve:
   $$\text{quadTo}(x_{i-1}, y_{i-1}, \frac{x_{i-1} + x_i}{2}, \frac{y_{i-1} + y_i}{2})$$
4. The path is drawn with anti-aliasing, rounded caps/joins, and iOS blue accent transparency (`#66007AFF`).

---

## 2. Path Recognition Algorithm

```mermaid
flowchart LR
    A[Touch Points (X, Y)] --> B[Spatial Key Crossing Detection]
    B --> C[Extract Start Char & End Char]
    C --> D[Retrieve Prefix Candidates from Trie]
    D --> E[Order Verification & Closeness Scoring]
    E --> F[Top 3 Candidate Words]
```

1. **Spatial Key Crossing:** Bounding boxes of keys along the trajectory are tested. When the finger enters a new letter key, that character is appended to the traversed sequence.
2. **Anchor Matching:** The first letter touched serves as the hard start anchor; the final letter touched serves as the end anchor.
3. **Trie Filtering:** Only dictionary words starting with the start letter and ending with the end letter are inspected.
4. **Sub-sequence Verification:** A candidate word must have its characters appear in topological order along the traversed letter path.
5. **Score Formulation:** Words are ranked by:
   $$\text{Score} = (\text{Frequency} \times 2.0) - (|L_{\text{traversed}} - L_{\text{word}}| \times 10)$$
6. Releasing the finger (`ACTION_UP`) instantly commits the top candidate word followed by a space, and feeds the word into the offline learning engine.
