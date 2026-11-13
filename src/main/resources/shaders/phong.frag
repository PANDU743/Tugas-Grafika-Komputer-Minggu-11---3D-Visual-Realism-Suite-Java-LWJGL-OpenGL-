// ==========================================================
// FILE: src/main/resources/shaders/phong.frag (Fragment Shader)
// Phong / Blinn-Phong per-pixel + Texture Mapping + Shadow Mapping (PCF 3x3)
// ==========================================================
#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform sampler2D textureSampler;   // unit 0
uniform sampler2D shadowMap;        // unit 1
uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadow;
uniform float specularStrength;
uniform float biasMin;              // b ; bias = max(10*b*(1 - N.L), b)  (rasio 0.05 : 0.005 seperti modul)

// Mengembalikan 0.0 (terang) .. 1.0 (bayangan penuh)
float calcShadow(vec4 fragPosLS, vec3 norm, vec3 lightDir) {
    vec3 proj = fragPosLS.xyz / fragPosLS.w;   // perspective divide
    proj = proj * 0.5 + 0.5;                   // NDC [-1,1] -> [0,1]
    if (proj.z > 1.0) return 0.0;              // di luar far plane cahaya
    float bias = max(10.0 * biasMin * (1.0 - dot(norm, lightDir)), biasMin);
    float shadow = 0.0;
    vec2 texel = 1.0 / vec2(textureSize(shadowMap, 0));
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float dShadow = texture(shadowMap, proj.xy + vec2(x, y) * texel).r;
            shadow += (proj.z - bias > dShadow) ? 1.0 : 0.0;   // d_current > d_shadow + bias
        }
    }
    return shadow / 9.0;
}

void main() {
    // 1. Ambient Lighting
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse Lighting (Lambertian)
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular Lighting (Phong vs Blinn-Phong)
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (useBlinnPhong) {
        vec3 halfwayDir = normalize(lightDir + viewDir);
        spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0); // Blinn-Phong
    } else {
        vec3 reflectDir = reflect(-lightDir, norm);
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Standard Phong
    }
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Shadow factor: 1.0 = terkena cahaya, 0.2 = dalam bayangan
    float shadowFactor = 1.0;
    if (useShadow) {
        shadowFactor = 1.0 - 0.8 * calcShadow(FragPosLightSpace, norm, lightDir);
    }

    // Combine Texture & Lighting (ambient tidak ikut tergelapkan bayangan)
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    vec3 result = (ambient + shadowFactor * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}
