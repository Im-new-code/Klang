precision mediump float;
varying vec2 vTexCoord;
uniform vec2 uResolution;
uniform vec2 uParallax;
uniform float uHour;
uniform float uTime;

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}
float noise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
        mix(hash21(i + vec2(0.0, 1.0)), hash21(i + vec2(1.0, 1.0)), f.x), f.y);
}
float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += noise2(p) * amplitude;
        p = p * 2.03 + vec2(9.2, 3.7);
        amplitude *= 0.5;
    }
    return value;
}
void main() {
    vec2 uv = vTexCoord;
    uv.x = clamp(uv.x - uParallax.x * 0.055, 0.001, 0.999);
    uv.y = clamp(uv.y + uParallax.y * 0.035, 0.001, 0.999);

    float nightFromEvening = smoothstep(17.8, 21.3, uHour);
    float nightFromMorning = 1.0 - smoothstep(4.6, 7.4, uHour);
    float night = max(nightFromEvening, nightFromMorning);
    float sunset = exp(-pow((uHour - 18.5) / 1.55, 2.0)) + exp(-pow((uHour - 6.2) / 1.4, 2.0));
    sunset = clamp(sunset, 0.0, 1.0);

    vec3 dayTop = vec3(0.23, 0.53, 0.66);
    vec3 dayHorizon = vec3(0.88, 0.72, 0.52);
    vec3 nightTop = vec3(0.025, 0.075, 0.15);
    vec3 nightHorizon = vec3(0.20, 0.24, 0.34);
    vec3 skyDay = mix(dayHorizon, dayTop, smoothstep(0.16, 0.95, uv.y));
    vec3 skyNight = mix(nightHorizon, nightTop, smoothstep(0.12, 0.97, uv.y));
    vec3 color = mix(skyDay, skyNight, night);
    color = mix(color, vec3(0.93, 0.48, 0.33), sunset * (1.0 - night) * (1.0 - smoothstep(0.08, 0.75, uv.y)));

    float cloudNoise = fbm(vec2(uv.x * 5.0 + uTime * 0.0004, uv.y * 2.5));
    float cloudBand = smoothstep(0.47, 0.72, cloudNoise) * smoothstep(0.37, 0.76, uv.y);
    cloudBand *= 1.0 - night * 0.45;
    vec3 cloudTint = mix(vec3(0.97, 0.89, 0.73), vec3(0.40, 0.52, 0.66), night);
    color = mix(color, cloudTint, cloudBand * 0.50);

    float sunX = 0.25 + 0.47 * smoothstep(5.0, 20.0, uHour);
    vec2 sunPos = vec2(sunX, 0.61);
    float sunDisc = 1.0 - smoothstep(0.035, 0.046, length((uv - sunPos) * vec2(uResolution.x / uResolution.y, 1.0)));
    color = mix(color, vec3(1.0, 0.79, 0.49), sunDisc * (1.0 - night * 0.7));

    float star = step(0.997, hash21(floor(uv * vec2(180.0, 100.0))));
    star *= smoothstep(0.35, 0.80, uv.y) * night;
    color += vec3(0.68, 0.83, 1.0) * star * (0.5 + 0.5 * sin(uTime * 0.01 + uv.x * 60.0));

    float farRidge = 0.38 + 0.045 * sin(uv.x * 10.0 + 0.4) + 0.035 * sin(uv.x * 23.0 + 2.1);
    farRidge += (fbm(vec2(uv.x * 9.0, 0.2)) - 0.5) * 0.065;
    float farMask = 1.0 - smoothstep(farRidge - 0.004, farRidge + 0.004, uv.y);
    vec3 farHill = mix(vec3(0.34, 0.49, 0.47), vec3(0.10, 0.19, 0.27), night);
    color = mix(color, farHill, farMask);

    float midRidge = 0.28 + 0.045 * sin(uv.x * 8.0 + 1.4) + 0.025 * sin(uv.x * 18.0);
    midRidge += (fbm(vec2(uv.x * 14.0, 4.0)) - 0.5) * 0.035;
    float midMask = 1.0 - smoothstep(midRidge - 0.006, midRidge + 0.006, uv.y);
    vec3 midHill = mix(vec3(0.22, 0.39, 0.30), vec3(0.055, 0.13, 0.16), night);
    color = mix(color, midHill, midMask);

    float nearRidge = 0.145 + 0.035 * sin(uv.x * 7.0 + 2.8) + 0.020 * sin(uv.x * 18.0 + 0.6);
    float nearMask = 1.0 - smoothstep(nearRidge - 0.006, nearRidge + 0.006, uv.y);
    vec3 nearHill = mix(vec3(0.12, 0.28, 0.20), vec3(0.035, 0.085, 0.075), night);
    color = mix(color, nearHill, nearMask);

    float poolDist = length((uv - vec2(0.55, 0.105)) * vec2(1.08, 4.8));
    float poolMask = 1.0 - smoothstep(0.28, 0.31, poolDist);
    float ripple = sin(poolDist * 93.0 - uTime * 0.035 + fbm(uv * 30.0) * 1.5);
    vec3 water = mix(vec3(0.13, 0.44, 0.48), vec3(0.035, 0.13, 0.20), night);
    water += vec3(0.10, 0.17, 0.16) * smoothstep(0.35, 0.9, ripple) * 0.28;
    color = mix(color, water, poolMask * 0.82);
    color += vec3(0.12, 0.24, 0.20) * poolMask * pow(max(ripple, 0.0), 10.0) * 0.35;

    float grain = (hash21(floor(uv * uResolution * 0.35)) - 0.5) * 0.035;
    float vignette = 1.0 - smoothstep(0.2, 1.0, length((uv - 0.5) * vec2(0.78, 0.92)));
    color += grain;
    color *= 0.88 + vignette * 0.12;
    gl_FragColor = vec4(color, 1.0);
}
