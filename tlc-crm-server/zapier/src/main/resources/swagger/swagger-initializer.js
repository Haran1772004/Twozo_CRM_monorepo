window.onload = () => {
  const ui = SwaggerUIBundle({
    url: "swigger-ui/swagger.json", // ← relative path to your swagger.json file
    dom_id: '#swagger-ui',
    deepLinking: true,
    presets: [
      SwaggerUIBundle.presets.apis,
      SwaggerUIStandalonePreset
    ],
    layout: "StandaloneLayout"
  });
};
