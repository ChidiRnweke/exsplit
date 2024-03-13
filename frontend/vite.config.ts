import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vitest/config';
import { purgeCss } from 'vite-plugin-tailwind-purgecss';
import Icons from 'unplugin-icons/vite'


export default defineConfig({
	plugins: [sveltekit(), Icons({ compiler: 'svelte' }), purgeCss()],
	test: {
		include: ['src/**/*.{test,spec}.{js,ts}']
	},
});
