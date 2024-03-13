/** @type {import('tailwindcss').Config} */
export default {
	content: ['./src/**/*.{html,js,svelte,ts}', "./src/app.html", './node_modules/flowbite-svelte/**/*.{html,js,svelte,ts}', "./node_modules/flowbite-svelte-icons/**/*.{html,js,svelte,ts}",
	],
	plugins: [require('flowbite/plugin')],
	safelist: [
		'p-4', 'p-8', 'p-16', "flex", "w-full", "max-w-md", "max-w-xl"	
	  ],
	
  
  
	darkMode: 'class',
	theme: {
	  extend: {
		fontFamily: {
		  sans: ['Roboto', 'sans-serif']
		},
		colors: {
		  primary: '#1a73e8',
		  secondary: '#0f62fe',
		  accent: '#4db8ac',
		  background: '#FFFFFF',
		  'background-alt': '#f0f0f0',
		  foreground: '#333333',
		  subtle: '#606060',
		  error: '#dc3545',
		  'dark-primary': '#BB86FC',
		  'dark-accent': '#03DAC5',
		  'dark-secondary': '#C792EA',
		  'dark-background': '#1F1B24',
		  'dark-background-alt': '#2C2C2C',
		  'dark-foreground': '#ffffff',
		  'dark-subtle': '#b0b0b0',
		  'dark-error': '#ff6b6b'
		},
  
	  }
	}
  }
  
  