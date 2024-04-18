import { sveltekit } from '@sveltejs/kit/vite';
import { defineConfig } from 'vitest/config';
import { purgeCss } from 'vite-plugin-tailwind-purgecss';
import Icons from 'unplugin-icons/vite';

export default defineConfig({
	server: {
		proxy: {
			'/api': 'http://localhost:9000',
			'/docs': 'http://localhost:9000'
		}
	},
	plugins: [sveltekit(), Icons({ compiler: 'svelte' }), purgeCss()],
	test: {
		include: ['src/**/*.{test,spec}.{js,ts}']
	}
});
