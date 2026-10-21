precision mediump float;
/* FBO reflection pass: render the scene first, draw the puddle afterward, enable alpha blending. */
uniform sampler2D uSceneTexture;
uniform float uTime;
uniform float uWaterLine;
uniform float uStrength;
varying vec2 vTexCoord;
void main() {
    vec2 uv = vTexCoord;
    float distanceBelowLine = max(0.0, uWaterLine - uv.y);
    vec2 reflectedUv = vec2(uv.x, 2.0 * uWaterLine - uv.y);
    float waveA = sin(uv.x * 27.0 + uTime * 1.7);
    float waveB = sin(uv.x * 43.0 - uTime * 1.1 + uv.y * 12.0);
    float distortion = (waveA * 0.55 + waveB * 0.45) * uStrength;
    reflectedUv.x += distortion * smoothstep(0.0, 0.16, distanceBelowLine);
    reflectedUv.y += sin(uv.x * 36.0 + uTime * 1.25) * uStrength * 0.35;
    reflectedUv = clamp(reflectedUv, vec2(0.002), vec2(0.998));
    vec3 reflected = texture2D(uSceneTexture, reflectedUv).rgb;
    float ripple = 0.5 + 0.5 * sin(uv.x * 60.0 + uv.y * 19.0 + uTime * 2.0);
    vec3 coolTint = reflected * vec3(0.72, 0.90, 0.94) + vec3(0.04, 0.12, 0.14) * ripple;
    float edgeDistance = min(min(uv.x, 1.0 - uv.x), min(distanceBelowLine, 0.20));
    float alpha = smoothstep(0.0, 0.035, edgeDistance) * 0.82;
    gl_FragColor = vec4(mix(vec3(0.035, 0.11, 0.14), coolTint, 0.88), alpha);
}
